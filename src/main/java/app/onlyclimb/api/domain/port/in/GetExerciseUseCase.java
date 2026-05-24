package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.Exercise;

import java.util.UUID;

public interface GetExerciseUseCase {
    /**
     * Returns an exercise visible to the (optionally null) caller.
     * Anonymous and non-owner callers only see PLATFORM or PUBLIC USER_CREATED
     * exercises. The owner can always see their own content.
     */
    Exercise getVisible(UUID exerciseId, UUID callerId);
}
