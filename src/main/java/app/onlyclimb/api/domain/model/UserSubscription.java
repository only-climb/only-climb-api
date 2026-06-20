package app.onlyclimb.api.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate representing a user's current subscription to a plan.
 * One active row per user (enforced by partial unique index on DB).
 * Status mirroring is driven by Stripe webhooks.
 */
public class UserSubscription {

    private final UUID id;
    private final UUID userId;
    private final UUID planId;
    private SubscriptionStatus status;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Instant trialEndsAt;
    private boolean cancelAtPeriodEnd;
    private Instant cancelledAt;
    private String paymentProvider;
    private String externalSubscriptionId;

    public UserSubscription(UUID id, UUID userId, UUID planId, SubscriptionStatus status,
                            Instant currentPeriodStart, Instant currentPeriodEnd,
                            Instant trialEndsAt, boolean cancelAtPeriodEnd, Instant cancelledAt,
                            String paymentProvider, String externalSubscriptionId) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.planId = Objects.requireNonNull(planId, "planId is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.currentPeriodStart = Objects.requireNonNull(currentPeriodStart, "currentPeriodStart is required");
        this.currentPeriodEnd = currentPeriodEnd;
        this.trialEndsAt = trialEndsAt;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        this.cancelledAt = cancelledAt;
        this.paymentProvider = paymentProvider;
        this.externalSubscriptionId = externalSubscriptionId;
        validateProviderRef();
    }

    /** Factory: provision a free lifetime subscription at registration. */
    public static UserSubscription provisionFree(UUID userId, UUID freePlanId) {
        Instant now = Instant.now();
        return new UserSubscription(
                UUID.randomUUID(), userId, freePlanId,
                SubscriptionStatus.ACTIVE, now, null, null, false, null, null, null);
    }

    public void activate(Instant periodStart, Instant periodEnd) {
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodStart = Objects.requireNonNull(periodStart);
        this.currentPeriodEnd = periodEnd;
        this.trialEndsAt = null;
        this.cancelAtPeriodEnd = false;
        this.cancelledAt = null;
    }

    public void markTrialing(Instant trialEnd) {
        this.status = SubscriptionStatus.TRIALING;
        this.trialEndsAt = Objects.requireNonNull(trialEnd);
    }

    public void cancel() {
        this.cancelAtPeriodEnd = true;
        this.cancelledAt = Instant.now();
    }

    public void reactivate() {
        if (this.status != SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Only cancelled subscriptions can be reactivated");
        }
        this.status = SubscriptionStatus.ACTIVE;
        this.cancelAtPeriodEnd = false;
        this.cancelledAt = null;
    }

    public void markPastDue() {
        this.status = SubscriptionStatus.PAST_DUE;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void markCancelled() {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    public void setPaymentProvider(String provider, String externalSubId) {
        this.paymentProvider = provider;
        this.externalSubscriptionId = externalSubId;
        validateProviderRef();
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING;
    }

    public boolean isTrialing() {
        return status == SubscriptionStatus.TRIALING;
    }

    public boolean isPremium() {
        return isActive() && !isFree();
    }

    /** A subscription without a payment provider is the internal free tier. */
    public boolean isFree() {
        return paymentProvider == null;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getPlanId() { return planId; }
    public SubscriptionStatus getStatus() { return status; }
    public Instant getCurrentPeriodStart() { return currentPeriodStart; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public Instant getTrialEndsAt() { return trialEndsAt; }
    public boolean isCancelAtPeriodEnd() { return cancelAtPeriodEnd; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getPaymentProvider() { return paymentProvider; }
    public String getExternalSubscriptionId() { return externalSubscriptionId; }

    private void validateProviderRef() {
        boolean hasProvider = paymentProvider != null;
        boolean hasExternalId = externalSubscriptionId != null;
        if (hasProvider != hasExternalId) {
            throw new IllegalArgumentException(
                    "paymentProvider and externalSubscriptionId must both be set or both be null");
        }
    }
}
