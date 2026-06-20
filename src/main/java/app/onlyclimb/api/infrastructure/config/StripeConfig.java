package app.onlyclimb.api.infrastructure.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Initialises the Stripe SDK with the configured API key.
 * The Stripe client is a static singleton; once set it is available everywhere.
 */
@Configuration
@EnableConfigurationProperties(StripeProperties.class)
@RequiredArgsConstructor
public class StripeConfig {

    private final StripeProperties stripeProperties;

    @PostConstruct
    void initStripe() {
        String key = stripeProperties.apiKey();
        if (key != null && !key.isBlank()) {
            Stripe.apiKey = key;
        }
    }
}
