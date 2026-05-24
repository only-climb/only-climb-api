package app.onlyclimb.api.domain.exception;

import java.util.UUID;

public class WorkoutTemplateNotFoundException extends RuntimeException {

    public WorkoutTemplateNotFoundException(UUID id) {
        super("Workout template not found: " + id);
    }
}
