package app.onlyclimb.api.infrastructure.adapter.in.web.follow;

import app.onlyclimb.api.domain.model.User;
import app.onlyclimb.api.domain.model.UserFollow;
import app.onlyclimb.api.domain.port.in.FollowUserUseCase;
import app.onlyclimb.api.domain.port.in.GetFollowStatsUseCase;
import app.onlyclimb.api.domain.port.in.ListFollowersUseCase;
import app.onlyclimb.api.domain.port.in.ListFollowingUseCase;
import app.onlyclimb.api.domain.port.in.ListFollowsQuery;
import app.onlyclimb.api.domain.port.in.UnfollowUserUseCase;
import app.onlyclimb.api.domain.port.out.UserFollowRepository;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.CurrentUserService;
import app.onlyclimb.api.infrastructure.adapter.in.web.follow.dto.FollowStatsResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.follow.dto.UserFollowEntryResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.follow.dto.UserFollowPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
public class UserFollowController {

    private final FollowUserUseCase followUseCase;
    private final UnfollowUserUseCase unfollowUseCase;
    private final GetFollowStatsUseCase statsUseCase;
    private final ListFollowersUseCase listFollowersUseCase;
    private final ListFollowingUseCase listFollowingUseCase;
    private final CurrentUserService currentUserService;

    @PostMapping("/follow")
    public ResponseEntity<Void> follow(
            @PathVariable UUID userId, Authentication authentication) {
        User caller = currentUserService.requireCurrent(authentication);
        followUseCase.follow(caller.getId(), userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/follow")
    public ResponseEntity<Void> unfollow(
            @PathVariable UUID userId, Authentication authentication) {
        User caller = currentUserService.requireCurrent(authentication);
        unfollowUseCase.unfollow(caller.getId(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/follow-stats")
    public ResponseEntity<FollowStatsResponse> stats(
            @PathVariable UUID userId, Authentication authentication) {
        UUID callerId = currentUserService.optionalCurrentId(authentication).orElse(null);
        return ResponseEntity.ok(FollowStatsResponse.from(statsUseCase.stats(userId, callerId)));
    }

    @GetMapping("/followers")
    public ResponseEntity<UserFollowPageResponse> followers(
            @PathVariable UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        UserFollowRepository.Page<UserFollow> page = listFollowersUseCase
                .listFollowers(new ListFollowsQuery(userId, cursor, limit));
        List<UserFollowEntryResponse> data = page.items().stream()
                .map(UserFollowEntryResponse::fromFollower).toList();
        return ResponseEntity.ok(new UserFollowPageResponse(data, page.nextCursor()));
    }

    @GetMapping("/following")
    public ResponseEntity<UserFollowPageResponse> following(
            @PathVariable UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        UserFollowRepository.Page<UserFollow> page = listFollowingUseCase
                .listFollowing(new ListFollowsQuery(userId, cursor, limit));
        List<UserFollowEntryResponse> data = page.items().stream()
                .map(UserFollowEntryResponse::fromFollowing).toList();
        return ResponseEntity.ok(new UserFollowPageResponse(data, page.nextCursor()));
    }
}
