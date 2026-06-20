package app.onlyclimb.api.domain.port.out;

/**
 * Output port for payment gateway operations (Stripe).
 * All Stripe-specific code lives in the infrastructure adapter.
 */
public interface PaymentGatewayPort {

    /** Result of creating a Stripe Checkout Session. */
    record CheckoutSessionResult(String sessionId, String checkoutUrl) {}

    /** Result of creating a Stripe Customer Portal session. */
    record PortalSessionResult(String portalUrl) {}

    /** Result of constructing a webhook event from raw payload + signature. */
    record WebhookEvent(String type, String eventId, String payloadJson) {}

    /**
     * Create a Stripe Checkout Session for a new subscription.
     * @param customerId Stripe customer id (gets or creates one)
     * @param priceId Stripe price id from the plan's externalRef
     * @param successUrl redirect on success
     * @param cancelUrl redirect on cancel
     */
    CheckoutSessionResult createCheckoutSession(String customerId, String priceId,
                                                String successUrl, String cancelUrl);

    /**
     * Create a Stripe Customer Portal session for plan management.
     * @param customerId Stripe customer id
     * @param returnUrl URL to redirect back to
     */
    PortalSessionResult createCustomerPortalSession(String customerId, String returnUrl);

    /**
     * Cancel a subscription in Stripe at period end.
     * @param externalSubscriptionId Stripe subscription id
     */
    void cancelSubscription(String externalSubscriptionId);

    /**
     * Verify the webhook signature and construct a typed event.
     * @param payload raw request body
     * @param sigHeader Stripe-Signature header value
     * @param webhookSecret configured signing secret
     * @throws app.onlyclimb.api.domain.exception.WebhookSignatureException if invalid
     */
    WebhookEvent constructWebhookEvent(String payload, String sigHeader, String webhookSecret);

    /**
     * Create a Stripe Customer for a user.
     * @param email user's email
     * @param userId local user UUID (stored as metadata)
     * @return Stripe customer id
     */
    String createCustomer(String email, String userId);
}
