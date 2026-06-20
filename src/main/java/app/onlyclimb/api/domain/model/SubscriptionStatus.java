package app.onlyclimb.api.domain.model;

/**
 * Mirrors the PostgreSQL {@code subscription_status} enum.
 * Domain-safe: no framework coupling.
 */
public enum SubscriptionStatus {
    TRIALING,
    ACTIVE,
    PAST_DUE,
    CANCELLED,
    EXPIRED;

    public boolean isTerminal() {
        return this == CANCELLED || this == EXPIRED;
    }

    public boolean isActiveOrTrialing() {
        return this == ACTIVE || this == TRIALING;
    }
}
