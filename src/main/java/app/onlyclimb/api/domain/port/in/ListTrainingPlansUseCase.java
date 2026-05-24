package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.TrainingPlan;
import app.onlyclimb.api.domain.port.out.TrainingPlanRepository;

public interface ListTrainingPlansUseCase {
    TrainingPlanRepository.Page<TrainingPlan> list(ListTrainingPlansQuery query);
}
