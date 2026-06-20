package app.onlyclimb.api.infrastructure.adapter.out.payment;

import app.onlyclimb.api.domain.exception.WebhookSignatureException;
import app.onlyclimb.api.domain.port.out.PaymentGatewayPort;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Stripe implementation of {@link PaymentGatewayPort}.
 * Delegates all Stripe SDK calls to the static Stripe client.
 */
@Component
@RequiredArgsConstructor
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    private final app.onlyclimb.api.infrastructure.config.StripeProperties stripeProperties;

    @Override
    public CheckoutSessionResult createCheckoutSession(String customerId, String priceId,
                                                       String successUrl, String cancelUrl) {
        try {
            com.stripe.param.checkout.SessionCreateParams params =
                    com.stripe.param.checkout.SessionCreateParams.builder()
                    .setCustomer(customerId)
                    .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build())
                    .build();

            Session session = Session.create(params);
            return new CheckoutSessionResult(session.getId(), session.getUrl());
        } catch (com.stripe.exception.StripeException e) {
            throw new RuntimeException("Stripe Checkout Session creation failed", e);
        }
    }

    @Override
    public PortalSessionResult createCustomerPortalSession(String customerId, String returnUrl) {
        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                    .setCustomer(customerId)
                    .setReturnUrl(returnUrl != null ? returnUrl : stripeProperties.portalReturnUrl())
                    .build();

            com.stripe.model.billingportal.Session session =
                    com.stripe.model.billingportal.Session.create(params);
            return new PortalSessionResult(session.getUrl());
        } catch (com.stripe.exception.StripeException e) {
            throw new RuntimeException("Stripe Customer Portal session creation failed", e);
        }
    }

    @Override
    public void cancelSubscription(String externalSubscriptionId) {
        try {
            com.stripe.model.Subscription subscription =
                    com.stripe.model.Subscription.retrieve(externalSubscriptionId);
            subscription.cancel();
        } catch (com.stripe.exception.StripeException e) {
            throw new RuntimeException("Stripe subscription cancellation failed", e);
        }
    }

    @Override
    public WebhookEvent constructWebhookEvent(String payload, String sigHeader, String webhookSecret) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            return new WebhookEvent(event.getType(), event.getId(), payload);
        } catch (SignatureVerificationException e) {
            throw new WebhookSignatureException("Invalid Stripe webhook signature", e);
        }
    }

    @Override
    public String createCustomer(String email, String userId) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .putMetadata("user_id", userId)
                    .build();
            Customer customer = Customer.create(params);
            return customer.getId();
        } catch (com.stripe.exception.StripeException e) {
            throw new RuntimeException("Stripe customer creation failed", e);
        }
    }
}
