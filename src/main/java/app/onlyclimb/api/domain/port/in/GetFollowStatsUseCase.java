package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.FollowStats;

import java.util.UUID;

public interface GetFollowStatsUseCase {

    /**
     * Aggregate stats for {@code targetUserId}. When {@code callerId} is non-null and differs
     * from the target, the response includes whether the caller follows the target.
     */
    FollowStats stats(UUID targetUserId, UUID callerId);
}
