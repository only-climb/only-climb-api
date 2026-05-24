package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.UserFollow;
import app.onlyclimb.api.domain.port.out.UserFollowRepository;

public interface ListFollowingUseCase {

    /** Page of users that {@code query.targetUserId()} follows, most-recent first. */
    UserFollowRepository.Page<UserFollow> listFollowing(ListFollowsQuery query);
}
