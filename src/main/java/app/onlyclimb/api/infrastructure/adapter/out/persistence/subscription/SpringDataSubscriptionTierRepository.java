package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataSubscriptionTierRepository extends JpaRepository<SubscriptionTierJpaEntity, Long> {

    Optional<SubscriptionTierJpaEntity> findByUuid(UUID uuid);

    Optional<SubscriptionTierJpaEntity> findByCode(String code);

    @Query("SELECT t FROM SubscriptionTierJpaEntity t WHERE t.active = true ORDER BY t.sortOrder ASC")
    java.util.List<SubscriptionTierJpaEntity> findAllActive();
}
