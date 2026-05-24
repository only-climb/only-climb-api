package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.Exercise;
import app.onlyclimb.api.domain.port.out.ExerciseRepository.Page;

public interface ListExercisesUseCase {

    Page<Exercise> list(ListExercisesQuery query);
}
