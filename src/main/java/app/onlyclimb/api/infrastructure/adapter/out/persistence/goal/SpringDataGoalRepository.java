package app.onlyclimb.api.infrastructure.adapter.out.persistence.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataGoalRepository extends JpaRepository<GoalJpaEntity, Long> {

    Optional<GoalJpaEntity> findByUuid(UUID uuid);

    Optional<GoalJpaEntity> findFirstByUserIdAndActiveTrue(Long userId);
}
