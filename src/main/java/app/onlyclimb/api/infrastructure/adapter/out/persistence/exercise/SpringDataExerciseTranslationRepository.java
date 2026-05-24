package app.onlyclimb.api.infrastructure.adapter.out.persistence.exercise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataExerciseTranslationRepository
        extends JpaRepository<ExerciseTranslationJpaEntity, Long> {

    List<ExerciseTranslationJpaEntity> findByExerciseId(Long exerciseId);

    void deleteByExerciseId(Long exerciseId);
}
