package app.onlyclimb.api.infrastructure.adapter.out.persistence.trainingplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataTrainingPlanWeekTranslationRepository
        extends JpaRepository<TrainingPlanWeekTranslationJpaEntity, Long> {

    List<TrainingPlanWeekTranslationJpaEntity> findByWeekIdIn(List<Long> weekIds);
}
