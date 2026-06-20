package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataSubscriptionPlanRepository extends JpaRepository<SubscriptionPlanJpaEntity, Long> {

    Optional<SubscriptionPlanJpaEntity> findByUuid(UUID uuid);

    @Query("SELECT p FROM SubscriptionPlanJpaEntity p JOIN FETCH p.tier t WHERE t.code = :tierCode AND p.active = true")
    List<SubscriptionPlanJpaEntity> findByTierCode(String tierCode);

    @Query("SELECT p FROM SubscriptionPlanJpaEntity p JOIN FETCH p.tier t WHERE p.active = true")
    List<SubscriptionPlanJpaEntity> findAllActive();
}
