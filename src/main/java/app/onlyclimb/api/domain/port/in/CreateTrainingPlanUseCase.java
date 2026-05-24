package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.TrainingPlan;

public interface CreateTrainingPlanUseCase {
    TrainingPlan create(CreateTrainingPlanCommand command);
}
