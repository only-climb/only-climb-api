package app.onlyclimb.api.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A concrete subscription SKU: tier × billing period × currency × price.
 * Linked to a Stripe price via {@code externalRef}.
 */
public class SubscriptionPlan {

    private final UUID id;
    private final UUID tierId;
    private final String tierCode; // denormalised for convenience
    private final BillingPeriod billingPeriod;
    private final int priceCents;
    private final String currency;
    private final String externalRef; // Stripe price id (e.g. price_xxx)
    private final boolean active;

    public SubscriptionPlan(UUID id, UUID tierId, String tierCode, BillingPeriod billingPeriod,
                            int priceCents, String currency, String externalRef, boolean active) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.tierId = Objects.requireNonNull(tierId, "tierId is required");
        this.tierCode = Objects.requireNonNull(tierCode, "tierCode is required");
        this.billingPeriod = Objects.requireNonNull(billingPeriod, "billingPeriod is required");
        if (priceCents < 0) throw new IllegalArgumentException("priceCents must be >= 0");
        this.priceCents = priceCents;
        this.currency = Objects.requireNonNull(currency, "currency is required");
        this.externalRef = externalRef;
        this.active = active;
    }

    public boolean isLifetime() {
        return billingPeriod == BillingPeriod.LIFETIME;
    }

    public boolean isFree() {
        return priceCents == 0;
    }

    public UUID getId() { return id; }
    public UUID getTierId() { return tierId; }
    public String getTierCode() { return tierCode; }
    public BillingPeriod getBillingPeriod() { return billingPeriod; }
    public int getPriceCents() { return priceCents; }
    public String getCurrency() { return currency; }
    public String getExternalRef() { return externalRef; }
    public boolean isActive() { return active; }
}
