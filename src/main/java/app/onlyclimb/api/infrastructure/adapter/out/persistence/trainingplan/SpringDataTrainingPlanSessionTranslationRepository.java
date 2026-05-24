package app.onlyclimb.api.infrastructure.adapter.out.persistence.trainingplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataTrainingPlanSessionTranslationRepository
        extends JpaRepository<TrainingPlanSessionTranslationJpaEntity, Long> {

    List<TrainingPlanSessionTranslationJpaEntity> findBySessionIdIn(List<Long> sessionIds);
}
