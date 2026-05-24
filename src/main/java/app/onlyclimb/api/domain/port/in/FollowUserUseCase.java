package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public interface FollowUserUseCase {

    /**
     * Make {@code follower} start following {@code following}. Idempotent: a no-op when the
     * edge already exists.
     *
     * @return {@code true} when a new edge was inserted, {@code false} when it already existed
     */
    boolean follow(UUID follower, UUID following);
}
