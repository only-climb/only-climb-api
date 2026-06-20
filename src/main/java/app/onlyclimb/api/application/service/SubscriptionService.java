package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.SubscriptionNotFoundException;
import app.onlyclimb.api.domain.model.FeatureLimits;
import app.onlyclimb.api.domain.model.PaymentCustomer;
import app.onlyclimb.api.domain.model.SubscriptionInvoice;
import app.onlyclimb.api.domain.model.SubscriptionPlan;
import app.onlyclimb.api.domain.model.UserSubscription;
import app.onlyclimb.api.domain.port.in.CreateCheckoutSessionCommand;
import app.onlyclimb.api.domain.port.in.CreateCheckoutSessionUseCase;
import app.onlyclimb.api.domain.port.in.CreateCustomerPortalSessionCommand;
import app.onlyclimb.api.domain.port.in.CreateCustomerPortalSessionUseCase;
import app.onlyclimb.api.domain.port.in.GetBillingHistoryUseCase;
import app.onlyclimb.api.domain.port.in.GetBillingHistoryUseCase.InvoicePage;
import app.onlyclimb.api.domain.port.in.GetBillingHistoryUseCase.InvoiceSummary;
import app.onlyclimb.api.domain.port.in.GetCurrentSubscriptionUseCase;
import app.onlyclimb.api.domain.port.in.GetSubscriptionTiersUseCase;
import app.onlyclimb.api.domain.port.in.ListInvoicesQuery;
import app.onlyclimb.api.domain.port.in.ProvisionFreeSubscriptionUseCase;
import app.onlyclimb.api.domain.port.out.PaymentCustomerRepository;
import app.onlyclimb.api.domain.port.out.PaymentGatewayPort;
import app.onlyclimb.api.domain.port.out.SubscriptionInvoiceRepository;
import app.onlyclimb.api.domain.port.out.SubscriptionPlanRepository;
import app.onlyclimb.api.domain.port.out.SubscriptionTierRepository;
import app.onlyclimb.api.domain.port.out.UserRepository;
import app.onlyclimb.api.domain.port.out.UserSubscriptionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService
        implements GetSubscriptionTiersUseCase, GetCurrentSubscriptionUseCase,
                   CreateCheckoutSessionUseCase, CreateCustomerPortalSessionUseCase,
                   GetBillingHistoryUseCase, ProvisionFreeSubscriptionUseCase {

    private static final String STRIPE_PROVIDER = "STRIPE";

    private final SubscriptionTierRepository tierRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PaymentCustomerRepository customerRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final PaymentGatewayPort paymentGateway;
    private final UserRepository userRepository;

    // ─── Tiers ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TierWithPlans> getTiers() {
        return tierRepository.findAllActive().stream()
                .sorted(Comparator.comparingInt(t -> t.getSortOrder()))
                .map(tier -> {
                    List<TierPlanSummary> plans = planRepository.findByTierCode(tier.getCode()).stream()
                            .map(p -> new TierPlanSummary(p.getId(), p.getBillingPeriod(),
                                    p.getPriceCents(), p.getCurrency()))
                            .toList();
                    return new TierWithPlans(tier, plans);
                })
                .toList();
    }

    // ─── Current subscription ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CurrentSubscription getCurrent(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .map(sub -> {
                    SubscriptionPlan plan = planRepository.findById(sub.getPlanId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Plan not found for subscription " + sub.getId()));
                    return CurrentSubscription.from(sub, plan.getTierCode());
                })
                .orElseThrow(() -> new SubscriptionNotFoundException(userId));
    }

    // ─── Checkout session ───────────────────────────────────────────────────

    @Override
    @Transactional
    public CheckoutSessionResponse create(UUID userId, CreateCheckoutSessionCommand command) {
        SubscriptionPlan plan = planRepository.findById(command.planId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Plan not found: " + command.planId()));

        if (!plan.isActive()) {
            throw new IllegalArgumentException("Plan is not active: " + command.planId());
        }

        if (plan.isFree()) {
            throw new IllegalArgumentException("Cannot create checkout for free plan");
        }

        String stripeCustomerId = getOrCreateStripeCustomer(userId);

        PaymentGatewayPort.CheckoutSessionResult result = paymentGateway.createCheckoutSession(
                stripeCustomerId,
                plan.getExternalRef(),
                command.successUrl(),
                command.cancelUrl());

        return new CheckoutSessionResponse(result.checkoutUrl());
    }

    // ─── Customer portal ────────────────────────────────────────────────────

    @Override
    @Transactional
    public PortalSessionResponse create(UUID userId, CreateCustomerPortalSessionCommand command) {
        String stripeCustomerId = getStripeCustomer(userId);

        PaymentGatewayPort.PortalSessionResult result = paymentGateway.createCustomerPortalSession(
                stripeCustomerId, command.returnUrl());

        return new PortalSessionResponse(result.portalUrl());
    }

    // ─── Billing history ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public InvoicePage getBillingHistory(ListInvoicesQuery query) {
        List<SubscriptionInvoice> invoices = invoiceRepository.findByUserId(
                query.userId(), query.limit(), query.cursor());

        List<InvoiceSummary> items = invoices.stream()
                .map(inv -> new InvoiceSummary(
                        inv.getId(), inv.getStatus(), inv.getAmountCents(),
                        inv.getAmountRefundedCents(), inv.getCurrency(),
                        inv.getPeriodStart(), inv.getPeriodEnd(),
                        inv.getIssuedAt(), inv.getPaidAt(),
                        inv.getHostedInvoiceUrl(), inv.getInvoicePdfUrl()))
                .toList();

        UUID nextCursor = items.size() == query.limit() && !items.isEmpty()
                ? items.getLast().invoiceId() : null;

        return new InvoicePage(items, nextCursor);
    }

    // ─── Free provisioning ──────────────────────────────────────────────────

    @Override
    @Transactional
    public void provision(UUID userId) {
        // Find the FREE plan
        SubscriptionPlan freePlan = planRepository.findByTierCode("FREE").stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("FREE plan not seeded"));

        UserSubscription sub = UserSubscription.provisionFree(userId, freePlan.getId());
        subscriptionRepository.save(sub);
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private String getOrCreateStripeCustomer(UUID userId) {
        return customerRepository.findByUserIdAndProvider(userId, STRIPE_PROVIDER)
                .map(PaymentCustomer::getExternalCustomerId)
                .orElseGet(() -> {
                    var user = userRepository.findById(userId)
                            .orElseThrow(() -> new app.onlyclimb.api.domain.exception.UserNotFoundException(userId));
                    String stripeId = paymentGateway.createCustomer(
                            user.getEmail().value(), userId.toString());
                    PaymentCustomer customer = new PaymentCustomer(
                            UUID.randomUUID(), userId, STRIPE_PROVIDER, stripeId);
                    customerRepository.save(customer);
                    return stripeId;
                });
    }

    private String getStripeCustomer(UUID userId) {
        return customerRepository.findByUserIdAndProvider(userId, STRIPE_PROVIDER)
                .map(PaymentCustomer::getExternalCustomerId)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "No Stripe customer for user " + userId));
    }
}
