package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.Exercise;

public interface UpdateExerciseUseCase {
    Exercise update(UpdateExerciseCommand command);
}
