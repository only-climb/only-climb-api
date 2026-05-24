package app.onlyclimb.api.infrastructure.adapter.out.persistence.trainingplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataTrainingPlanWeekRepository
        extends JpaRepository<TrainingPlanWeekJpaEntity, Long> {

    List<TrainingPlanWeekJpaEntity> findByPlanIdOrderByWeekNumber(Long planId);

    void deleteByPlanId(Long planId);
}
