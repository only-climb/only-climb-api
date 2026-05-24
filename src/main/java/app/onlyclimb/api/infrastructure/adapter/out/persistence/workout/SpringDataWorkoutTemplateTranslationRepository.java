package app.onlyclimb.api.infrastructure.adapter.out.persistence.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataWorkoutTemplateTranslationRepository
        extends JpaRepository<WorkoutTemplateTranslationJpaEntity, Long> {

    List<WorkoutTemplateTranslationJpaEntity> findByTemplateId(Long templateId);

    void deleteByTemplateId(Long templateId);
}
