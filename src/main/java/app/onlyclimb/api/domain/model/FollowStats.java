package app.onlyclimb.api.domain.model;

/**
 * Aggregate follower/following counts for a user, with the caller's relation.
 *
 * @param followersCount   total accounts following the target user
 * @param followingCount   total accounts the target user follows
 * @param followedByCaller {@code true} when the authenticated caller follows the target user;
 *                         {@code false} for self or anonymous queries
 */
public record FollowStats(long followersCount, long followingCount, boolean followedByCaller) {

    public FollowStats {
        if (followersCount < 0 || followingCount < 0) {
            throw new IllegalArgumentException("Follow counts cannot be negative");
        }
    }
}
