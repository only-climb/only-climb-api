package app.onlyclimb.api.infrastructure.adapter.out.persistence.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataWorkoutTemplateExerciseTranslationRepository
        extends JpaRepository<WorkoutTemplateExerciseTranslationJpaEntity, Long> {

    List<WorkoutTemplateExerciseTranslationJpaEntity> findByTemplateExerciseIdIn(List<Long> ids);
}
