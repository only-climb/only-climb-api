package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.TrainingPlan;

import java.util.UUID;

public interface GetTrainingPlanUseCase {
    /**
     * Returns the plan if it exists and is visible to {@code callerId} (which
     * may be {@code null} for anonymous reads).
     */
    TrainingPlan get(UUID planId, UUID callerId);
}
