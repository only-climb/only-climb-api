package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.GoalType;
import app.onlyclimb.api.domain.model.TrainingVolume;

import java.util.UUID;

/**
 * Query filter for listing training plans.
 *
 * <p>When {@code callerId} is null, only PLATFORM and PUBLIC USER_CREATED
 * plans are returned. When provided, the caller's own private plans are also
 * included.</p>
 */
public record ListTrainingPlansQuery(
        UUID callerId,
        DifficultyLevel difficulty,
        ClimbingDiscipline discipline,
        GoalType primaryGoal,
        TrainingVolume volume,
        String search,
        boolean ownedOnly,
        String cursor,
        int limit) {

    public ListTrainingPlansQuery {
        if (limit <= 0 || limit > 100) {
            limit = 20;
        }
        if (search != null && search.isBlank()) {
            search = null;
        }
        if (cursor != null && cursor.isBlank()) {
            cursor = null;
        }
    }
}
