package app.onlyclimb.api.infrastructure.adapter.out.persistence.trainingplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataTrainingPlanTranslationRepository
        extends JpaRepository<TrainingPlanTranslationJpaEntity, Long> {

    List<TrainingPlanTranslationJpaEntity> findByPlanId(Long planId);

    void deleteByPlanId(Long planId);
}
