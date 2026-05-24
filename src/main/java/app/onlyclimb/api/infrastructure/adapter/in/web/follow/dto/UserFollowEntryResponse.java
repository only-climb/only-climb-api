package app.onlyclimb.api.infrastructure.adapter.in.web.follow.dto;

import app.onlyclimb.api.domain.model.UserFollow;

import java.time.Instant;
import java.util.UUID;

/**
 * One edge from a paginated follow list. {@code userId} is the "other side" of the edge:
 * for a followers list it is the follower; for a following list it is the followee.
 */
public record UserFollowEntryResponse(UUID userId, Instant followedAt) {

    public static UserFollowEntryResponse fromFollower(UserFollow edge) {
        return new UserFollowEntryResponse(edge.followerId(), edge.createdAt());
    }

    public static UserFollowEntryResponse fromFollowing(UserFollow edge) {
        return new UserFollowEntryResponse(edge.followingId(), edge.createdAt());
    }
}
