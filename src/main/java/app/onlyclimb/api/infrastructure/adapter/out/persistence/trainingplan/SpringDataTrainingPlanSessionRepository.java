package app.onlyclimb.api.infrastructure.adapter.out.persistence.trainingplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataTrainingPlanSessionRepository
        extends JpaRepository<TrainingPlanSessionJpaEntity, Long> {

    List<TrainingPlanSessionJpaEntity> findByWeekIdInOrderByDayOfWeekAscPositionAsc(List<Long> weekIds);
}
