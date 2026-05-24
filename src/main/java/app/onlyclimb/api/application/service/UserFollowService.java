package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.CannotFollowSelfException;
import app.onlyclimb.api.domain.exception.UserNotFoundException;
import app.onlyclimb.api.domain.model.FollowStats;
import app.onlyclimb.api.domain.model.UserFollow;
import app.onlyclimb.api.domain.port.in.FollowUserUseCase;
import app.onlyclimb.api.domain.port.in.GetFollowStatsUseCase;
import app.onlyclimb.api.domain.port.in.ListFollowersUseCase;
import app.onlyclimb.api.domain.port.in.ListFollowingUseCase;
import app.onlyclimb.api.domain.port.in.ListFollowsQuery;
import app.onlyclimb.api.domain.port.in.UnfollowUserUseCase;
import app.onlyclimb.api.domain.port.out.UserFollowRepository;
import app.onlyclimb.api.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserFollowService implements
        FollowUserUseCase,
        UnfollowUserUseCase,
        GetFollowStatsUseCase,
        ListFollowersUseCase,
        ListFollowingUseCase {

    private final UserFollowRepository followRepository;
    private final UserRepository userRepository;

    @Override
    public boolean follow(UUID follower, UUID following) {
        Objects.requireNonNull(follower, "follower is required");
        Objects.requireNonNull(following, "following is required");
        if (follower.equals(following)) {
            throw new CannotFollowSelfException(follower);
        }
        requireUserExists(follower);
        requireUserExists(following);
        return followRepository.follow(follower, following);
    }

    @Override
    public boolean unfollow(UUID follower, UUID following) {
        Objects.requireNonNull(follower, "follower is required");
        Objects.requireNonNull(following, "following is required");
        if (follower.equals(following)) {
            return false;
        }
        return followRepository.unfollow(follower, following);
    }

    @Override
    public FollowStats stats(UUID targetUserId, UUID callerId) {
        Objects.requireNonNull(targetUserId, "targetUserId is required");
        requireUserExists(targetUserId);
        long followers = followRepository.countFollowers(targetUserId);
        long following = followRepository.countFollowing(targetUserId);
        boolean followedByCaller = callerId != null
                && !callerId.equals(targetUserId)
                && followRepository.isFollowing(callerId, targetUserId);
        return new FollowStats(followers, following, followedByCaller);
    }

    @Override
    public UserFollowRepository.Page<UserFollow> listFollowers(ListFollowsQuery query) {
        Objects.requireNonNull(query, "query is required");
        requireUserExists(query.targetUserId());
        return followRepository.findFollowers(query);
    }

    @Override
    public UserFollowRepository.Page<UserFollow> listFollowing(ListFollowsQuery query) {
        Objects.requireNonNull(query, "query is required");
        requireUserExists(query.targetUserId());
        return followRepository.findFollowing(query);
    }

    private void requireUserExists(UUID id) {
        if (userRepository.findById(id).isEmpty()) {
            throw new UserNotFoundException(id);
        }
    }
}
