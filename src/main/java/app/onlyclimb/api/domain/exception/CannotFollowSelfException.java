package app.onlyclimb.api.domain.exception;

import java.util.UUID;

/** Thrown when a user attempts to follow themselves. */
public class CannotFollowSelfException extends RuntimeException {

    public CannotFollowSelfException(UUID userId) {
        super("User " + userId + " cannot follow themselves");
    }
}
