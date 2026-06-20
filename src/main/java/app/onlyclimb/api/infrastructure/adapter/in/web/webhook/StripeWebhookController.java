package app.onlyclimb.api.infrastructure.adapter.in.web.webhook;

import app.onlyclimb.api.domain.exception.WebhookSignatureException;
import app.onlyclimb.api.domain.model.InvoiceStatus;
import app.onlyclimb.api.domain.model.PaymentCustomer;
import app.onlyclimb.api.domain.model.PaymentWebhookEvent;
import app.onlyclimb.api.domain.model.SubscriptionInvoice;
import app.onlyclimb.api.domain.port.out.PaymentCustomerRepository;
import app.onlyclimb.api.domain.port.out.PaymentGatewayPort;
import app.onlyclimb.api.domain.port.out.PaymentWebhookEventRepository;
import app.onlyclimb.api.domain.port.out.SubscriptionInvoiceRepository;
import app.onlyclimb.api.domain.port.out.UserSubscriptionRepository;
import app.onlyclimb.api.infrastructure.config.StripeProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Stripe webhook events.
 * <p>
 * Idempotency is guaranteed by the UNIQUE constraint on
 * (payment_provider, external_event_id) in {@code payment_webhook_events}.
 * Duplicate deliveries return 200 without side effects.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private static final String STRIPE_PROVIDER = "STRIPE";

    private final StripeProperties stripeProperties;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final PaymentCustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "Handle Stripe webhook events (idempotent)")
    public ResponseEntity<Void> handle(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        // 1. Verify signature and parse event
        PaymentGatewayPort.WebhookEvent stripeEvent;
        try {
            stripeEvent = paymentGateway.constructWebhookEvent(
                    payload, sigHeader, stripeProperties.webhookSecret());
        } catch (WebhookSignatureException e) {
            log.warn("Invalid Stripe webhook signature", e);
            return ResponseEntity.badRequest().build();
        }

        // 2. Idempotency check — at-most-once processing
        if (webhookEventRepository.existsByProviderAndExternalEventId(
                STRIPE_PROVIDER, stripeEvent.eventId())) {
            log.debug("Duplicate webhook event ignored: {}", stripeEvent.eventId());
            return ResponseEntity.ok().build();
        }

        // 3. Persist the raw event
        PaymentWebhookEvent domainEvent = PaymentWebhookEvent.received(
                UUID.randomUUID(), STRIPE_PROVIDER, stripeEvent.eventId(),
                stripeEvent.type(), stripeEvent.payloadJson());
        webhookEventRepository.save(domainEvent);

        // 4. Process the event
        try {
            processEvent(stripeEvent.type(), stripeEvent.payloadJson());
            domainEvent.markProcessed();
        } catch (Exception e) {
            log.error("Failed to process webhook event {}: {}", stripeEvent.eventId(), e.getMessage(), e);
            domainEvent.markFailed(e.getMessage());
        }
        webhookEventRepository.save(domainEvent);

        return ResponseEntity.ok().build();
    }

    private void processEvent(String eventType, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.path("data").path("object");

            switch (eventType) {
                case "checkout.session.completed" -> handleCheckoutCompleted(data);
                case "customer.subscription.updated" -> handleSubscriptionUpdated(data);
                case "customer.subscription.deleted" -> handleSubscriptionDeleted(data);
                case "invoice.paid" -> handleInvoicePaid(data);
                case "invoice.payment_failed" -> handleInvoicePaymentFailed(data);
                default -> log.debug("Unhandled Stripe event type: {}", eventType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process event " + eventType, e);
        }
    }

    private void handleCheckoutCompleted(JsonNode data) {
        String subscriptionId = data.path("subscription").asText();
        String customerId = data.path("customer").asText();
        if (subscriptionId.isEmpty()) {
            log.debug("Checkout session completed without subscription id — ignoring");
            return;
        }
        // The subscription will be fully processed by customer.subscription.updated
        log.info("Checkout completed for customer {} — subscription {}", customerId, subscriptionId);
    }

    private void handleSubscriptionUpdated(JsonNode data) {
        String externalSubId = data.path("id").asText();
        String status = data.path("status").asText();
        long periodStart = data.path("current_period_start").asLong();
        long periodEnd = data.path("current_period_end").asLong();
        Long trialEnd = data.path("trial_end").isNull() ? null : data.path("trial_end").asLong();
        boolean cancelAtPeriodEnd = data.path("cancel_at_period_end").asBoolean(false);

        subscriptionRepository.findByProviderAndExternalId(STRIPE_PROVIDER, externalSubId)
                .ifPresentOrElse(
                        sub -> {
                            switch (status) {
                                case "active" -> sub.activate(
                                        Instant.ofEpochSecond(periodStart),
                                        Instant.ofEpochSecond(periodEnd));
                                case "trialing" -> sub.markTrialing(
                                        trialEnd != null ? Instant.ofEpochSecond(trialEnd) : null);
                                case "past_due" -> sub.markPastDue();
                                case "canceled", "incomplete_expired" -> sub.markCancelled();
                                default -> log.debug("Unhandled subscription status: {}", status);
                            }
                            if (cancelAtPeriodEnd && !sub.isCancelAtPeriodEnd()) {
                                sub.cancel();
                            }
                            subscriptionRepository.save(sub);
                            log.info("Subscription {} updated to status {}", externalSubId, status);
                        },
                        () -> log.warn("Unknown subscription: {}", externalSubId));
    }

    private void handleSubscriptionDeleted(JsonNode data) {
        String externalSubId = data.path("id").asText();
        subscriptionRepository.findByProviderAndExternalId(STRIPE_PROVIDER, externalSubId)
                .ifPresent(sub -> {
                    sub.markCancelled();
                    subscriptionRepository.save(sub);
                    log.info("Subscription {} deleted", externalSubId);
                });
    }

    private void handleInvoicePaid(JsonNode data) {
        String externalInvoiceId = data.path("id").asText();
        String customerId = data.path("customer").asText();
        int amountPaid = data.path("amount_paid").asInt();
        String currency = data.path("currency").asText().toUpperCase();
        long periodStart = data.path("period_start").asLong();
        long periodEnd = data.path("period_end").asLong();
        String hostedUrl = data.path("hosted_invoice_url").asText();
        String pdfUrl = data.path("invoice_pdf").asText();

        UUID userId = customerRepository
                .findByProviderAndExternalId(STRIPE_PROVIDER, customerId)
                .map(PaymentCustomer::getUserId)
                .orElse(null);

        if (userId == null) {
            log.warn("No local user for Stripe customer {} — ignoring invoice {}", customerId, externalInvoiceId);
            return;
        }

        SubscriptionInvoice invoice = new SubscriptionInvoice(
                UUID.randomUUID(), userId, null, STRIPE_PROVIDER, externalInvoiceId,
                InvoiceStatus.PAID, amountPaid, 0, currency,
                periodStart > 0 ? Instant.ofEpochSecond(periodStart) : null,
                periodEnd > 0 ? Instant.ofEpochSecond(periodEnd) : null,
                Instant.now(), Instant.now(),
                pdfUrl.isEmpty() ? null : pdfUrl,
                hostedUrl.isEmpty() ? null : hostedUrl);
        invoiceRepository.save(invoice);
        log.info("Invoice {} paid: {} {} for user {}", externalInvoiceId, amountPaid, currency, userId);
    }

    private void handleInvoicePaymentFailed(JsonNode data) {
        String externalInvoiceId = data.path("id").asText();
        String subscriptionId = data.path("subscription").asText();
        log.warn("Invoice {} payment failed for subscription {}", externalInvoiceId, subscriptionId);

        if (!subscriptionId.isEmpty()) {
            subscriptionRepository.findByProviderAndExternalId(STRIPE_PROVIDER, subscriptionId)
                    .ifPresent(sub -> {
                        sub.markPastDue();
                        subscriptionRepository.save(sub);
                    });
        }
    }
}
