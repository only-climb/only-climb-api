package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.CannotFollowSelfException;
import app.onlyclimb.api.domain.exception.UserNotFoundException;
import app.onlyclimb.api.domain.model.AuthProvider;
import app.onlyclimb.api.domain.model.Email;
import app.onlyclimb.api.domain.model.FollowStats;
import app.onlyclimb.api.domain.model.User;
import app.onlyclimb.api.domain.port.in.ListFollowsQuery;
import app.onlyclimb.api.domain.port.out.UserFollowRepository;
import app.onlyclimb.api.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserFollowServiceTest {

    @Mock UserFollowRepository followRepository;
    @Mock UserRepository userRepository;
    @InjectMocks UserFollowService service;

    UUID alice;
    UUID bob;

    @BeforeEach
    void setUp() {
        alice = UUID.randomUUID();
        bob = UUID.randomUUID();
    }

    private void existingUsers(UUID... ids) {
        for (UUID id : ids) {
            given(userRepository.findById(id))
                    .willReturn(Optional.of(User.register(AuthProvider.CLERK, "ext-" + id, new Email(id + "@example.com"))));
        }
    }

    @Test
    void followDelegatesAndReturnsRepositoryResult() {
        existingUsers(alice, bob);
        given(followRepository.follow(alice, bob)).willReturn(true);

        assertThat(service.follow(alice, bob)).isTrue();
    }

    @Test
    void followRejectsSelf() {
        assertThatThrownBy(() -> service.follow(alice, alice))
                .isInstanceOf(CannotFollowSelfException.class);
        verify(followRepository, never()).follow(any(), any());
    }

    @Test
    void followRejectsUnknownTarget() {
        existingUsers(alice);
        given(userRepository.findById(bob)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.follow(alice, bob))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void unfollowIsIdempotentOnSelf() {
        assertThat(service.unfollow(alice, alice)).isFalse();
        verify(followRepository, never()).unfollow(any(), any());
    }

    @Test
    void unfollowDelegates() {
        given(followRepository.unfollow(alice, bob)).willReturn(true);
        assertThat(service.unfollow(alice, bob)).isTrue();
    }

    @Test
    void statsAggregatesCountsAndCallerRelation() {
        existingUsers(bob);
        given(followRepository.countFollowers(bob)).willReturn(10L);
        given(followRepository.countFollowing(bob)).willReturn(7L);
        given(followRepository.isFollowing(alice, bob)).willReturn(true);

        FollowStats stats = service.stats(bob, alice);

        assertThat(stats.followersCount()).isEqualTo(10L);
        assertThat(stats.followingCount()).isEqualTo(7L);
        assertThat(stats.followedByCaller()).isTrue();
    }

    @Test
    void statsSkipsCallerCheckWhenCallerIsTarget() {
        existingUsers(alice);
        given(followRepository.countFollowers(alice)).willReturn(1L);
        given(followRepository.countFollowing(alice)).willReturn(2L);

        FollowStats stats = service.stats(alice, alice);

        assertThat(stats.followedByCaller()).isFalse();
        verify(followRepository, never()).isFollowing(any(), any());
    }

    @Test
    void listFollowersValidatesTarget() {
        existingUsers(bob);
        ListFollowsQuery q = new ListFollowsQuery(bob, null, 20);
        given(followRepository.findFollowers(q))
                .willReturn(new UserFollowRepository.Page<>(List.of(), null));

        assertThat(service.listFollowers(q).items()).isEmpty();
    }

    @Test
    void listFollowingRejectsUnknownTarget() {
        given(userRepository.findById(bob)).willReturn(Optional.empty());
        ListFollowsQuery q = new ListFollowsQuery(bob, null, 20);
        assertThatThrownBy(() -> service.listFollowing(q))
                .isInstanceOf(UserNotFoundException.class);
    }

    private static UUID any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
