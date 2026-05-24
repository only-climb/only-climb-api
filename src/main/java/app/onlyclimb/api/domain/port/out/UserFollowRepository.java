package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.UserFollow;
import app.onlyclimb.api.domain.port.in.ListFollowsQuery;

import java.util.List;
import java.util.UUID;

/**
 * Output port for the user follow graph stored in {@code user_followers}.
 *
 * <p>Read operations take user UUIDs and translate to the underlying bigint ids internally.
 * Write operations are idempotent and return whether they mutated the graph.</p>
 */
public interface UserFollowRepository {

    /** Insert the edge if absent. Returns {@code true} when a new row was created. */
    boolean follow(UUID followerId, UUID followingId);

    /** Delete the edge if present. Returns {@code true} when a row was removed. */
    boolean unfollow(UUID followerId, UUID followingId);

    boolean isFollowing(UUID followerId, UUID followingId);

    long countFollowers(UUID userId);

    long countFollowing(UUID userId);

    Page<UserFollow> findFollowers(ListFollowsQuery query);

    Page<UserFollow> findFollowing(ListFollowsQuery query);

    record Page<T>(List<T> items, String nextCursor) {
        public Page {
            items = List.copyOf(items);
        }
    }
}
