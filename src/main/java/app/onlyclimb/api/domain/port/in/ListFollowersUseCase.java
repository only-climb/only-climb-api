package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.UserFollow;
import app.onlyclimb.api.domain.port.out.UserFollowRepository;

public interface ListFollowersUseCase {

    /** Page of users that follow {@code query.targetUserId()}, most-recent first. */
    UserFollowRepository.Page<UserFollow> listFollowers(ListFollowsQuery query);
}
