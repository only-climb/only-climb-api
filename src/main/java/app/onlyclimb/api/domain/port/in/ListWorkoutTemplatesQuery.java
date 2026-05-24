package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.DifficultyLevel;

import java.util.UUID;

/**
 * Query filter for listing workout templates.
 *
 * <p>When {@code callerId} is null, only PLATFORM and PUBLIC USER_CREATED
 * templates are returned. When provided, the caller's own private templates
 * are also included.</p>
 */
public record ListWorkoutTemplatesQuery(
        UUID callerId,
        DifficultyLevel difficulty,
        ClimbingDiscipline discipline,
        String search,
        boolean ownedOnly,
        String cursor,
        int limit) {

    public ListWorkoutTemplatesQuery {
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
