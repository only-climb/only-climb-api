package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.UserSubscription;

import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository {
    UserSubscription save(UserSubscription subscription);
    Optional<UserSubscription> findById(UUID id);
    Optional<UserSubscription> findActiveByUserId(UUID userId);
    Optional<UserSubscription> findByUserId(UUID userId);
    Optional<UserSubscription> findByProviderAndExternalId(String provider, String externalSubscriptionId);
}
