package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.WorkoutTemplate;

import java.util.UUID;

public interface ForkWorkoutTemplateUseCase {
    /**
     * Creates a new USER_CREATED template owned by {@code callerId} as a deep
     * copy of {@code sourceTemplateId}. The new template is PRIVATE by default
     * and its {@code forkedFromId} points at the source.
     */
    WorkoutTemplate fork(UUID sourceTemplateId, UUID callerId);
}
