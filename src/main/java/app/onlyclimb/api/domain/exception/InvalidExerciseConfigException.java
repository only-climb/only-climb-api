package app.onlyclimb.api.domain.exception;

/**
 * Thrown when a workout template entry declares a configuration parameter
 * that is not in the referenced exercise's allowed-parameters set.
 */
public class InvalidExerciseConfigException extends RuntimeException {

    public InvalidExerciseConfigException(String message) {
        super(message);
    }
}
