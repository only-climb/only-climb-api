package app.onlyclimb.api.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Canonical mapping of (user, payment_provider) → external customer id.
 * Survives plan changes. Never exposed to the client.
 */
public class PaymentCustomer {

    private final UUID id;
    private final UUID userId;
    private final String paymentProvider;
    private final String externalCustomerId;

    public PaymentCustomer(UUID id, UUID userId, String paymentProvider, String externalCustomerId) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.paymentProvider = Objects.requireNonNull(paymentProvider, "paymentProvider is required");
        this.externalCustomerId = Objects.requireNonNull(externalCustomerId, "externalCustomerId is required");
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getPaymentProvider() { return paymentProvider; }
    public String getExternalCustomerId() { return externalCustomerId; }
}
