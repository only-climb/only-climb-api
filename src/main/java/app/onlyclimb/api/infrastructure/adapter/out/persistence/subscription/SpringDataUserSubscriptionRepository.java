package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import app.onlyclimb.api.domain.model.SubscriptionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataUserSubscriptionRepository extends JpaRepository<UserSubscriptionJpaEntity, Long> {

    Optional<UserSubscriptionJpaEntity> findByUuid(UUID uuid);

    @Query("SELECT s FROM UserSubscriptionJpaEntity s JOIN FETCH s.plan p JOIN FETCH p.tier t WHERE s.uuid = :uuid")
    Optional<UserSubscriptionJpaEntity> findByUuidWithPlan(UUID uuid);

    @Query("SELECT s FROM UserSubscriptionJpaEntity s JOIN FETCH s.plan p JOIN FETCH p.tier t WHERE s.userId = :userId")
    List<UserSubscriptionJpaEntity> findByUserIdWithPlan(Long userId);

    @Query("SELECT s FROM UserSubscriptionJpaEntity s JOIN FETCH s.plan p JOIN FETCH p.tier t " +
           "WHERE s.userId = :userId AND s.status IN ('ACTIVE', 'TRIALING', 'PAST_DUE')")
    Optional<UserSubscriptionJpaEntity> findActiveByUserIdWithPlan(Long userId);

    Optional<UserSubscriptionJpaEntity> findByPaymentProviderAndExternalSubscriptionId(
            String paymentProvider, String externalSubscriptionId);
}
