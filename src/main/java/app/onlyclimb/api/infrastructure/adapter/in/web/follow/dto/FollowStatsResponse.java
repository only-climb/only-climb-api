package app.onlyclimb.api.infrastructure.adapter.in.web.follow.dto;

import app.onlyclimb.api.domain.model.FollowStats;

public record FollowStatsResponse(long followersCount, long followingCount, boolean followedByCaller) {

    public static FollowStatsResponse from(FollowStats stats) {
        return new FollowStatsResponse(
                stats.followersCount(), stats.followingCount(), stats.followedByCaller());
    }
}
