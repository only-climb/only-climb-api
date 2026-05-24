package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public record ListGoalsQuery(
        UUID ownerId,
        Boolean activeOnly,
        String cursor,
        int limit) {

    public ListGoalsQuery {
        if (limit <= 0 || limit > 100) {
            limit = 20;
        }
    }
}
