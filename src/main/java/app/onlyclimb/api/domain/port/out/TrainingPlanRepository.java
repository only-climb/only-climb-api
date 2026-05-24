package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.TrainingPlan;
import app.onlyclimb.api.domain.port.in.ListTrainingPlansQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingPlanRepository {

    TrainingPlan save(TrainingPlan plan);

    Optional<TrainingPlan> findById(UUID id);

    Page<TrainingPlan> search(ListTrainingPlansQuery query);

    /** A single page of results with an opaque continuation cursor. */
    record Page<T>(List<T> items, String nextCursor) {
        public Page {
            items = List.copyOf(items);
        }
    }
}
