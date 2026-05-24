package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.WorkoutTemplate;
import app.onlyclimb.api.domain.port.in.ListWorkoutTemplatesQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutTemplateRepository {

    WorkoutTemplate save(WorkoutTemplate template);

    Optional<WorkoutTemplate> findById(UUID id);

    Page<WorkoutTemplate> search(ListWorkoutTemplatesQuery query);

    /** A single page of results with an opaque continuation cursor. */
    record Page<T>(List<T> items, String nextCursor) {
        public Page {
            items = List.copyOf(items);
        }
    }
}
