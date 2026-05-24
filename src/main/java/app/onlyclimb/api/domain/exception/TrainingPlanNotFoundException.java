package app.onlyclimb.api.domain.exception;

import java.util.UUID;

public class TrainingPlanNotFoundException extends RuntimeException {

    public TrainingPlanNotFoundException(UUID id) {
        super("Training plan not found: " + id);
    }
}
