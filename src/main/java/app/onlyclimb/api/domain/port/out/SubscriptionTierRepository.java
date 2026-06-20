package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.SubscriptionTier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionTierRepository {
    List<SubscriptionTier> findAllActive();
    Optional<SubscriptionTier> findByCode(String code);
    Optional<SubscriptionTier> findById(UUID id);
}
