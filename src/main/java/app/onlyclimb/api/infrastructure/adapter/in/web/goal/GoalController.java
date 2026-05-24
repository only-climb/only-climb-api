package app.onlyclimb.api.infrastructure.adapter.in.web.goal;

import app.onlyclimb.api.domain.exception.GoalNotFoundException;
import app.onlyclimb.api.domain.model.Goal;
import app.onlyclimb.api.domain.port.in.AchieveGoalUseCase;
import app.onlyclimb.api.domain.port.in.CreateGoalCommand;
import app.onlyclimb.api.domain.port.in.CreateGoalUseCase;
import app.onlyclimb.api.domain.port.in.DeleteGoalUseCase;
import app.onlyclimb.api.domain.port.in.GetGoalUseCase;
import app.onlyclimb.api.domain.port.in.ListGoalsQuery;
import app.onlyclimb.api.domain.port.in.ListGoalsUseCase;
import app.onlyclimb.api.domain.port.in.UpdateGoalCommand;
import app.onlyclimb.api.domain.port.in.UpdateGoalUseCase;
import app.onlyclimb.api.domain.port.out.GoalRepository.Page;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.CurrentUserService;
import app.onlyclimb.api.infrastructure.adapter.in.web.goal.dto.CreateGoalRequest;
import app.onlyclimb.api.infrastructure.adapter.in.web.goal.dto.GoalPageResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.goal.dto.GoalResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.goal.dto.UpdateGoalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Tag(name = "Goals", description = "Structured training objectives per user")
public class GoalController {

    private final CreateGoalUseCase createUseCase;
    private final UpdateGoalUseCase updateUseCase;
    private final DeleteGoalUseCase deleteUseCase;
    private final AchieveGoalUseCase achieveUseCase;
    private final GetGoalUseCase getUseCase;
    private final ListGoalsUseCase listUseCase;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "List the caller's goals (history)")
    public ResponseEntity<GoalPageResponse> list(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") int limit,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        Page<Goal> page = listUseCase.list(new ListGoalsQuery(
                caller.getId(), activeOnly, cursor, limit));
        List<GoalResponse> data = page.items().stream().map(GoalResponse::from).toList();
        return ResponseEntity.ok(new GoalPageResponse(data, page.nextCursor()));
    }

    @GetMapping("/current")
    @Operation(summary = "Get the caller's currently active goal, if any")
    public ResponseEntity<GoalResponse> getCurrent(Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        return getUseCase.getCurrent(caller.getId())
                .map(g -> ResponseEntity.ok(GoalResponse.from(g)))
                .orElseThrow(() -> new GoalNotFoundException(caller.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of the caller's goals by UUID")
    public ResponseEntity<GoalResponse> getById(
            @PathVariable UUID id, Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        Goal goal = getUseCase.getOwned(id, caller.getId());
        return ResponseEntity.ok(GoalResponse.from(goal));
    }

    @PostMapping
    @Operation(summary = "Create a new active goal (supersedes the previous active one)")
    public ResponseEntity<GoalResponse> create(
            @Valid @RequestBody CreateGoalRequest request,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        Goal created = createUseCase.create(new CreateGoalCommand(
                caller.getId(),
                request.type(),
                request.targetGrade() == null ? null : request.targetGrade().toDomain(),
                request.targetDate(),
                request.notes()));
        return ResponseEntity
                .created(URI.create("/api/v1/goals/" + created.getId()))
                .body(GoalResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of the caller's goals")
    public ResponseEntity<GoalResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGoalRequest request,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        Goal updated = updateUseCase.update(new UpdateGoalCommand(
                id,
                caller.getId(),
                request.targetGrade() == null ? null : request.targetGrade().toDomain(),
                request.targetDate(),
                request.notes()));
        return ResponseEntity.ok(GoalResponse.from(updated));
    }

    @PostMapping("/{id}/achieve")
    @Operation(summary = "Mark one of the caller's goals as achieved")
    public ResponseEntity<GoalResponse> achieve(
            @PathVariable UUID id, Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        Goal achieved = achieveUseCase.achieve(id, caller.getId());
        return ResponseEntity.ok(GoalResponse.from(achieved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hard-delete one of the caller's goals")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        deleteUseCase.delete(id, caller.getId());
        return ResponseEntity.noContent().build();
    }
}
