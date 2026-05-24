package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.Exercise;
import app.onlyclimb.api.domain.port.in.ListExercisesQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseRepository {

    Exercise save(Exercise exercise);

    Optional<Exercise> findById(UUID id);

    Page<Exercise> search(ListExercisesQuery query);

    /** A single page of results with an opaque continuation cursor. */
    record Page<T>(List<T> items, String nextCursor) {
        public Page {
            items = List.copyOf(items);
        }
    }
}
