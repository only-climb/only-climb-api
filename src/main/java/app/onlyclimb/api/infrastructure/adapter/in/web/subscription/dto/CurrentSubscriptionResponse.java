package app.onlyclimb.api.infrastructure.adapter.in.web.subscription.dto;

import app.onlyclimb.api.domain.port.in.GetCurrentSubscriptionUseCase.CurrentSubscription;

import java.time.Instant;
import java.util.UUID;

public record CurrentSubscriptionResponse(
        UUID subscriptionId,
        UUID planId,
        String tierCode,
        String status,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Instant trialEndsAt,
        boolean cancelAtPeriodEnd,
        boolean isFree
) {
    public static CurrentSubscriptionResponse from(CurrentSubscription sub) {
        return new CurrentSubscriptionResponse(
                sub.subscriptionId(),
                sub.planId(),
                sub.tierCode(),
                sub.status().name(),
                sub.currentPeriodStart(),
                sub.currentPeriodEnd(),
                sub.trialEndsAt(),
                sub.cancelAtPeriodEnd(),
                sub.isFree());
    }
}
