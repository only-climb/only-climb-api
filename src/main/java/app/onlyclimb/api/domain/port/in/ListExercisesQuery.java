package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

/**
 * Query filter for listing exercises.
 *
 * <p>When {@code callerId} is null, only PLATFORM and PUBLIC USER_CREATED
 * exercises are returned. When provided, the caller's own private exercises
 * are also included.</p>
 */
public record ListExercisesQuery(
        UUID callerId,
        String categoryCode,
        String search,
        boolean ownedOnly,
        String cursor,
        int limit) {

    public ListExercisesQuery {
        if (limit <= 0 || limit > 100) {
            limit = 20;
        }
        if (search != null && search.isBlank()) {
            search = null;
        }
        if (categoryCode != null && categoryCode.isBlank()) {
            categoryCode = null;
        }
        if (cursor != null && cursor.isBlank()) {
            cursor = null;
        }
    }
}
