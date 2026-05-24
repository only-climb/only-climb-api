package app.onlyclimb.api.domain.exception;

/** Thrown when a caller attempts to mutate immutable platform-authored content. */
public class PlatformContentImmutableException extends RuntimeException {

    public PlatformContentImmutableException(String message) {
        super(message);
    }
}
