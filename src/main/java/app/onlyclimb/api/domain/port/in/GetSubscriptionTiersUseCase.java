package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.SubscriptionTier;

import java.util.List;

public interface GetSubscriptionTiersUseCase {
    /** Returns active tiers ordered by sortOrder, each with its plans. */
    record TierWithPlans(SubscriptionTier tier, List<TierPlanSummary> plans) {}

    record TierPlanSummary(java.util.UUID planId, app.onlyclimb.api.domain.model.BillingPeriod billingPeriod,
                           int priceCents, String currency) {}

    List<TierWithPlans> getTiers();
}
