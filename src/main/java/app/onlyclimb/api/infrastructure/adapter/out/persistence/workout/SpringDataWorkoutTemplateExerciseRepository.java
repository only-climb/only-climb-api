package app.onlyclimb.api.infrastructure.adapter.out.persistence.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataWorkoutTemplateExerciseRepository
        extends JpaRepository<WorkoutTemplateExerciseJpaEntity, Long> {

    List<WorkoutTemplateExerciseJpaEntity> findByTemplateIdOrderByPosition(Long templateId);

    void deleteByTemplateId(Long templateId);
}
