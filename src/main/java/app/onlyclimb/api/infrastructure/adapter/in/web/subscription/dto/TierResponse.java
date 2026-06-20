package app.onlyclimb.api.infrastructure.adapter.in.web.subscription.dto;

import app.onlyclimb.api.domain.model.SubscriptionTier;
import app.onlyclimb.api.domain.port.in.GetSubscriptionTiersUseCase.TierPlanSummary;
import app.onlyclimb.api.domain.port.in.GetSubscriptionTiersUseCase.TierWithPlans;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TierResponse(
        UUID id,
        String code,
        int sortOrder,
        Map<String, Map<String, String>> translations,
        List<PlanSummary> plans
) {
    public record PlanSummary(
            UUID planId,
            String billingPeriod,
            int priceCents,
            String currency
    ) {}

    public static TierResponse from(TierWithPlans twp) {
        SubscriptionTier tier = twp.tier();
        List<PlanSummary> planSummaries = twp.plans().stream()
                .map(p -> new PlanSummary(p.planId(), p.billingPeriod().name(),
                        p.priceCents(), p.currency()))
                .toList();
        return new TierResponse(tier.getId(), tier.getCode(), tier.getSortOrder(),
                tier.getTranslations(), planSummaries);
    }

    public static List<TierResponse> fromList(List<TierWithPlans> tiers) {
        return tiers.stream().map(TierResponse::from).toList();
    }
}
