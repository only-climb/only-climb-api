package app.onlyclimb.api.domain.exception;

/** Thrown when a caller is not the owner of a piece of user-created content. */
public class ContentOwnershipException extends RuntimeException {

    public ContentOwnershipException(String message) {
        super(message);
    }
}
