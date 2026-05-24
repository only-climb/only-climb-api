package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.TrainingPlan;

public interface UpdateTrainingPlanUseCase {
    TrainingPlan update(UpdateTrainingPlanCommand command);
}
