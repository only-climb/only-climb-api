package app.onlyclimb.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Stripe integration.
 *
 * <ul>
 *   <li>{@code apiKey}: the {@code sk_live_*} or {@code sk_test_*} secret key.</li>
 *   <li>{@code webhookSecret}: the {@code whsec_*} signing secret for Stripe webhooks.</li>
 *   <li>{@code portalReturnUrl}: the URL Stripe redirects to after the Customer Portal session.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "onlyclimb.stripe")
public record StripeProperties(
        String apiKey,
        String webhookSecret,
        String portalReturnUrl) {
}
