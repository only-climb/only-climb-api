package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import app.onlyclimb.api.domain.model.SubscriptionStatus;
import app.onlyclimb.api.domain.model.UserSubscription;
import app.onlyclimb.api.domain.port.out.UserSubscriptionRepository;
import app.onlyclimb.api.infrastructure.adapter.out.persistence.user.SpringDataUserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UserSubscriptionJpaAdapter implements UserSubscriptionRepository {

    private final SpringDataUserSubscriptionRepository springRepo;
    private final SpringDataUserRepository userSpringRepo;
    private final SpringDataSubscriptionPlanRepository springPlanRepo;

    @Override
    public UserSubscription save(UserSubscription subscription) {
        Long userInternalId = resolveUserInternalId(subscription.getUserId());

        UserSubscriptionJpaEntity entity = springRepo.findByUuid(subscription.getId())
                .orElseGet(UserSubscriptionJpaEntity::new);

        if (entity.getId() == null) {
            entity.setUuid(subscription.getId());
            entity.setCreatedAt(java.time.Instant.now());
            entity.setUserId(userInternalId);
        }
        entity.setUpdatedAt(java.time.Instant.now());

        // Resolve plan entity by UUID
        SubscriptionPlanJpaEntity planRef = springPlanRepo.findByUuid(subscription.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Plan not found: " + subscription.getPlanId()));
        entity.setPlan(planRef);

        entity.setStatus(subscription.getStatus());
        entity.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        entity.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        entity.setTrialEndsAt(subscription.getTrialEndsAt());
        entity.setCancelAtPeriodEnd(subscription.isCancelAtPeriodEnd());
        entity.setCancelledAt(subscription.getCancelledAt());
        entity.setPaymentProvider(subscription.getPaymentProvider());
        entity.setExternalSubscriptionId(subscription.getExternalSubscriptionId());

        return toDomain(springRepo.save(entity));
    }

    @Override
    public Optional<UserSubscription> findById(UUID id) {
        return springRepo.findByUuidWithPlan(id).map(this::toDomain);
    }

    @Override
    public Optional<UserSubscription> findActiveByUserId(UUID userId) {
        return resolveUserInternalIdOptional(userId)
                .flatMap(springRepo::findActiveByUserIdWithPlan)
                .map(this::toDomain);
    }

    @Override
    public Optional<UserSubscription> findByUserId(UUID userId) {
        Long internalId = resolveUserInternalId(userId);
        var list = springRepo.findByUserIdWithPlan(internalId);
        return list.isEmpty() ? Optional.empty() : Optional.of(toDomain(list.getFirst()));
    }

    @Override
    public Optional<UserSubscription> findByProviderAndExternalId(String provider, String externalSubscriptionId) {
        return springRepo.findByPaymentProviderAndExternalSubscriptionId(provider, externalSubscriptionId)
                .map(this::toDomain);
    }

    private Long resolveUserInternalId(UUID userUuid) {
        return userSpringRepo.findByUuid(userUuid)
                .orElseThrow(() -> new app.onlyclimb.api.domain.exception.UserNotFoundException(userUuid))
                .getId();
    }

    private Optional<Long> resolveUserInternalIdOptional(UUID userUuid) {
        return userSpringRepo.findByUuid(userUuid)
                .map(user -> user.getId());
    }

    private UserSubscription toDomain(UserSubscriptionJpaEntity entity) {
        // Resolve user UUID from internal ID
        UUID userUuid = userSpringRepo.findById(entity.getUserId())
                .map(u -> u.getUuid())
                .orElseThrow(() -> new IllegalStateException("User not found for id " + entity.getUserId()));

        return new UserSubscription(
                entity.getUuid(),
                userUuid,
                entity.getPlan().getUuid(),
                entity.getStatus() != null ? entity.getStatus() : SubscriptionStatus.ACTIVE,
                entity.getCurrentPeriodStart(),
                entity.getCurrentPeriodEnd(),
                entity.getTrialEndsAt(),
                entity.isCancelAtPeriodEnd(),
                entity.getCancelledAt(),
                entity.getPaymentProvider(),
                entity.getExternalSubscriptionId());
    }
}
