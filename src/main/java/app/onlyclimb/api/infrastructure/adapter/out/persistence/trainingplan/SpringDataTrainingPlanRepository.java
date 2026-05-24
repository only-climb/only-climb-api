package app.onlyclimb.api.infrastructure.adapter.out.persistence.trainingplan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

interface SpringDataTrainingPlanRepository extends
        JpaRepository<TrainingPlanJpaEntity, Long>,
        JpaSpecificationExecutor<TrainingPlanJpaEntity> {

    Optional<TrainingPlanJpaEntity> findByUuid(UUID uuid);
}
