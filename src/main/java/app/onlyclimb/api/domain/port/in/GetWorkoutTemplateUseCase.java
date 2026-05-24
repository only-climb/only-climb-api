package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.WorkoutTemplate;

import java.util.UUID;

public interface GetWorkoutTemplateUseCase {
    /**
     * Returns a template visible to the (optionally null) caller.
     * Anonymous and non-owner callers only see PLATFORM or PUBLIC USER_CREATED
     * templates. The owner always sees their own templates.
     */
    WorkoutTemplate getVisible(UUID templateId, UUID callerId);
}
