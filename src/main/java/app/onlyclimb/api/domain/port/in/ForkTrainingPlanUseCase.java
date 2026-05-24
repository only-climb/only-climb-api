package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.TrainingPlan;

import java.util.UUID;

public interface ForkTrainingPlanUseCase {
    /**
     * Deep-copies the source plan as a brand new USER_CREATED, PRIVATE plan
     * owned by {@code callerId}. The new plan's {@code forkedFromId} points at
     * the source and its {@code generationType} is
     * {@link app.onlyclimb.api.domain.model.PlanGenerationType#FORKED}.
     */
    TrainingPlan fork(UUID sourcePlanId, UUID callerId);
}
