package app.onlyclimb.api.domain.port.in;

import java.util.Objects;
import java.util.UUID;

/**
 * Query parameters for listing followers/following of a target user.
 *
 * @param targetUserId user being inspected (the {@code followee} for followers,
 *                     the {@code follower} for following)
 * @param cursor       opaque pagination cursor (nullable for first page)
 * @param limit        page size; clamped to {@code [1, 100]}; defaults to {@code 20}
 */
public record ListFollowsQuery(UUID targetUserId, String cursor, int limit) {

    public ListFollowsQuery {
        Objects.requireNonNull(targetUserId, "targetUserId is required");
        if (limit <= 0 || limit > 100) {
            limit = 20;
        }
    }
}
