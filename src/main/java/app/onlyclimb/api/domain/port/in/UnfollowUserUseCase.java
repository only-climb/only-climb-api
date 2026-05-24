package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public interface UnfollowUserUseCase {

    /**
     * Remove the follow edge from {@code follower} to {@code following}. Idempotent.
     *
     * @return {@code true} when an edge was removed, {@code false} when no edge existed
     */
    boolean unfollow(UUID follower, UUID following);
}
