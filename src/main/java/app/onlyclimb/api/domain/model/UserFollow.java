package app.onlyclimb.api.domain.model;

import app.onlyclimb.api.domain.exception.CannotFollowSelfException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Directed edge in the user follow graph.
 *
 * <p>Represents the fact that {@code followerId} follows {@code followingId}
 * starting at {@code createdAt}. Self-follows are rejected at construction.</p>
 */
public record UserFollow(UUID followerId, UUID followingId, Instant createdAt) {

    public UserFollow {
        Objects.requireNonNull(followerId, "followerId is required");
        Objects.requireNonNull(followingId, "followingId is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        if (followerId.equals(followingId)) {
            throw new CannotFollowSelfException(followerId);
        }
    }
}
