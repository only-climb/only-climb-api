package app.onlyclimb.api.infrastructure.adapter.out.persistence.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

interface SpringDataExerciseRepository extends
        JpaRepository<ExerciseJpaEntity, Long>,
        JpaSpecificationExecutor<ExerciseJpaEntity> {

    Optional<ExerciseJpaEntity> findByUuid(UUID uuid);
}
