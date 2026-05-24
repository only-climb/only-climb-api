package app.onlyclimb.api.infrastructure.adapter.out.persistence.workout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

interface SpringDataWorkoutTemplateRepository extends
        JpaRepository<WorkoutTemplateJpaEntity, Long>,
        JpaSpecificationExecutor<WorkoutTemplateJpaEntity> {

    Optional<WorkoutTemplateJpaEntity> findByUuid(UUID uuid);
}
