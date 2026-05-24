package app.onlyclimb.api.infrastructure.adapter.in.web.follow;

import app.onlyclimb.api.domain.exception.CannotFollowSelfException;
import app.onlyclimb.api.domain.exception.UserNotFoundException;
import app.onlyclimb.api.domain.model.AuthProvider;
import app.onlyclimb.api.domain.model.Email;
import app.onlyclimb.api.domain.model.FollowStats;
import app.onlyclimb.api.domain.model.User;
import app.onlyclimb.api.domain.model.UserFollow;
import app.onlyclimb.api.domain.port.in.FollowUserUseCase;
import app.onlyclimb.api.domain.port.in.GetFollowStatsUseCase;
import app.onlyclimb.api.domain.port.in.GetUserUseCase;
import app.onlyclimb.api.domain.port.in.ListFollowersUseCase;
import app.onlyclimb.api.domain.port.in.ListFollowingUseCase;
import app.onlyclimb.api.domain.port.in.ListFollowsQuery;
import app.onlyclimb.api.domain.port.in.UnfollowUserUseCase;
import app.onlyclimb.api.domain.port.out.UserFollowRepository;
import app.onlyclimb.api.domain.port.out.UserRepository;
import app.onlyclimb.api.infrastructure.adapter.in.web.GlobalExceptionHandler;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.ClerkJwtAuthenticationConverter;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.CurrentUserService;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.UserAuthorization;
import app.onlyclimb.api.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserFollowController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        UserAuthorization.class,
        CurrentUserService.class
})
class UserFollowControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean FollowUserUseCase followUseCase;
    @MockitoBean UnfollowUserUseCase unfollowUseCase;
    @MockitoBean GetFollowStatsUseCase statsUseCase;
    @MockitoBean ListFollowersUseCase listFollowersUseCase;
    @MockitoBean ListFollowingUseCase listFollowingUseCase;
    @MockitoBean GetUserUseCase getUserUseCase;
    @MockitoBean UserRepository userRepository;
    @MockitoBean ClerkJwtAuthenticationConverter clerkJwtAuthenticationConverter;

    private User authenticate() {
        User caller = User.register(AuthProvider.CLERK, "ext-1", new Email("alice@example.com"));
        when(getUserUseCase.getByAuthIdentity(AuthProvider.CLERK, "ext-1")).thenReturn(caller);
        return caller;
    }

    @Test
    void followRequiresJwt() throws Exception {
        UUID target = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/users/{id}/follow", target))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void followReturnsNoContent() throws Exception {
        User caller = authenticate();
        UUID target = UUID.randomUUID();
        given(followUseCase.follow(caller.getId(), target)).willReturn(true);

        mockMvc.perform(post("/api/v1/users/{id}/follow", target)
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void followSelfReturnsBadRequest() throws Exception {
        User caller = authenticate();
        willThrow(new CannotFollowSelfException(caller.getId()))
                .given(followUseCase).follow(eq(caller.getId()), eq(caller.getId()));

        mockMvc.perform(post("/api/v1/users/{id}/follow", caller.getId())
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void followUnknownTargetReturnsNotFound() throws Exception {
        User caller = authenticate();
        UUID target = UUID.randomUUID();
        willThrow(new UserNotFoundException(target))
                .given(followUseCase).follow(eq(caller.getId()), eq(target));

        mockMvc.perform(post("/api/v1/users/{id}/follow", target)
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unfollowReturnsNoContent() throws Exception {
        User caller = authenticate();
        UUID target = UUID.randomUUID();
        given(unfollowUseCase.unfollow(caller.getId(), target)).willReturn(true);

        mockMvc.perform(delete("/api/v1/users/{id}/follow", target)
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void statsExposesCounts() throws Exception {
        User caller = authenticate();
        UUID target = UUID.randomUUID();
        given(statsUseCase.stats(target, caller.getId()))
                .willReturn(new FollowStats(42, 17, true));

        mockMvc.perform(get("/api/v1/users/{id}/follow-stats", target)
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followersCount").value(42))
                .andExpect(jsonPath("$.followingCount").value(17))
                .andExpect(jsonPath("$.followedByCaller").value(true));
    }

    @Test
    void followersPageProjectsOtherSide() throws Exception {
        authenticate();
        UUID target = UUID.randomUUID();
        UUID follower = UUID.randomUUID();
        UserFollow edge = new UserFollow(follower, target, Instant.parse("2026-01-01T00:00:00Z"));
        given(listFollowersUseCase.listFollowers(any()))
                .willReturn(new UserFollowRepository.Page<>(List.of(edge), "next"));

        mockMvc.perform(get("/api/v1/users/{id}/followers", target)
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(follower.toString()))
                .andExpect(jsonPath("$.nextCursor").value("next"));
    }

    @Test
    void followingPageProjectsOtherSide() throws Exception {
        authenticate();
        UUID target = UUID.randomUUID();
        UUID followee = UUID.randomUUID();
        UserFollow edge = new UserFollow(target, followee, Instant.parse("2026-01-01T00:00:00Z"));
        given(listFollowingUseCase.listFollowing(any()))
                .willReturn(new UserFollowRepository.Page<>(List.of(edge), null));

        mockMvc.perform(get("/api/v1/users/{id}/following", target)
                        .with(jwt().jwt(j -> j.subject("ext-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(followee.toString()));
    }
}
