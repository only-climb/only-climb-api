package app.onlyclimb.api.domain.exception;

import java.util.UUID;

public class GoalNotFoundException extends RuntimeException {
    public GoalNotFoundException(UUID id) {
        super("Goal not found: " + id);
    }
}
