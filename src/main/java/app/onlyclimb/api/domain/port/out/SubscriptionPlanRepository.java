package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.SubscriptionPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository {
    List<SubscriptionPlan> findByTierCode(String tierCode);
    List<SubscriptionPlan> findAllActive();
    Optional<SubscriptionPlan> findById(UUID id);
}
