package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.SubscriptionStatus;
import app.onlyclimb.api.domain.model.UserSubscription;

import java.time.Instant;
import java.util.UUID;

public interface GetCurrentSubscriptionUseCase {
    /** Returns the user's current subscription with tier info. */
    record CurrentSubscription(
            UUID subscriptionId,
            UUID planId,
            String tierCode,
            SubscriptionStatus status,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            Instant trialEndsAt,
            boolean cancelAtPeriodEnd,
            boolean isFree
    ) {
        public static CurrentSubscription from(UserSubscription sub, String tierCode) {
            return new CurrentSubscription(
                    sub.getId(), sub.getPlanId(), tierCode,
                    sub.getStatus(), sub.getCurrentPeriodStart(),
                    sub.getCurrentPeriodEnd(), sub.getTrialEndsAt(),
                    sub.isCancelAtPeriodEnd(), sub.isFree());
        }
    }

    CurrentSubscription getCurrent(UUID userId);
}
