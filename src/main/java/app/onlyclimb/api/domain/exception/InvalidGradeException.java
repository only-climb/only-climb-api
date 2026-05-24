package app.onlyclimb.api.domain.exception;

import app.onlyclimb.api.domain.model.ClimbingGrade;

public class InvalidGradeException extends RuntimeException {
    public InvalidGradeException(String message) {
        super(message);
    }

    public InvalidGradeException(ClimbingGrade grade) {
        super("Unknown climbing grade: " + grade);
    }
}
