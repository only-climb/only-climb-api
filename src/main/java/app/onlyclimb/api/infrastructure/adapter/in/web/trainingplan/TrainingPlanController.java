package app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan;

import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.GoalType;
import app.onlyclimb.api.domain.model.GradeScale;
import app.onlyclimb.api.domain.model.TrainingPlan;
import app.onlyclimb.api.domain.model.TrainingVolume;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.port.in.CreateTrainingPlanCommand;
import app.onlyclimb.api.domain.port.in.CreateTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.DeleteTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.EquipmentRequirementInput;
import app.onlyclimb.api.domain.port.in.ForkTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.GetTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.ListTrainingPlansQuery;
import app.onlyclimb.api.domain.port.in.ListTrainingPlansUseCase;
import app.onlyclimb.api.domain.port.in.TrainingPlanSessionInput;
import app.onlyclimb.api.domain.port.in.TrainingPlanWeekInput;
import app.onlyclimb.api.domain.port.in.UpdateTrainingPlanCommand;
import app.onlyclimb.api.domain.port.in.UpdateTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.out.TrainingPlanRepository.Page;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.CurrentUserService;
import app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto.ClimbingGradeDto;
import app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto.CreateTrainingPlanRequest;
import app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto.EquipmentRequirementRequest;
import app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto.TrainingPlanPageResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto.TrainingPlanResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto.TrainingPlanSessionNotesDto;
import app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto.TrainingPlanSessionRequest;
import app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto.TrainingPlanTranslationDto;
import app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto.TrainingPlanWeekRequest;
import app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto.TrainingPlanWeekTranslationDto;
import app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto.UpdateTrainingPlanRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/training-plans")
@RequiredArgsConstructor
@Tag(name = "Training plans", description = "Multi-week training programs")
public class TrainingPlanController {

    private final CreateTrainingPlanUseCase createUseCase;
    private final UpdateTrainingPlanUseCase updateUseCase;
    private final DeleteTrainingPlanUseCase deleteUseCase;
    private final GetTrainingPlanUseCase getUseCase;
    private final ListTrainingPlansUseCase listUseCase;
    private final ForkTrainingPlanUseCase forkUseCase;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "List training plans visible to the caller")
    public ResponseEntity<TrainingPlanPageResponse> list(
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) ClimbingDiscipline discipline,
            @RequestParam(required = false) GoalType primaryGoal,
            @RequestParam(required = false) TrainingVolume volume,
            @RequestParam(required = false) String search,
            @RequestParam(name = "owned", defaultValue = "false") boolean ownedOnly,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        UUID callerId = currentUserService.optionalCurrentId(authentication).orElse(null);
        Page<TrainingPlan> page = listUseCase.list(new ListTrainingPlansQuery(
                callerId, difficulty, discipline, primaryGoal, volume, search,
                ownedOnly, cursor, limit));
        String locale = resolveLocale(acceptLanguage);
        List<TrainingPlanResponse> data = page.items().stream()
                .map(p -> TrainingPlanResponse.from(p, locale))
                .toList();
        return ResponseEntity.ok(new TrainingPlanPageResponse(data, page.nextCursor()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a training plan by UUID")
    public ResponseEntity<TrainingPlanResponse> getById(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        UUID callerId = currentUserService.optionalCurrentId(authentication).orElse(null);
        TrainingPlan plan = getUseCase.get(id, callerId);
        return ResponseEntity.ok(TrainingPlanResponse.from(plan, resolveLocale(acceptLanguage)));
    }

    @PostMapping
    @Operation(summary = "Create a new user-authored training plan")
    public ResponseEntity<TrainingPlanResponse> create(
            @Valid @RequestBody CreateTrainingPlanRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        TrainingPlan created = createUseCase.create(new CreateTrainingPlanCommand(
                caller.getId(),
                request.visibility(),
                request.difficultyLevel(),
                request.targetDiscipline(),
                request.primaryGoal(),
                request.secondaryGoals() == null ? Set.of() : request.secondaryGoals(),
                toGrade(request.targetGradeMin()),
                toGrade(request.targetGradeMax()),
                request.durationWeeks(),
                request.sessionsPerWeek(),
                request.avgSessionDurationMinutes(),
                request.trainingVolume(),
                request.requiresHangboard(),
                request.requiresCampusBoard(),
                request.requiresGymAccess(),
                request.requiresOutdoorClimbing(),
                request.recoveryFocused(),
                toEquipment(request.equipment()),
                toWeeks(request.weeks()),
                toTranslations(request.translations())));
        return ResponseEntity
                .created(URI.create("/api/v1/training-plans/" + created.getId()))
                .body(TrainingPlanResponse.from(created, resolveLocale(acceptLanguage)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of the caller's training plans")
    public ResponseEntity<TrainingPlanResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTrainingPlanRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        TrainingPlan updated = updateUseCase.update(new UpdateTrainingPlanCommand(
                id,
                caller.getId(),
                request.visibility(),
                request.difficultyLevel(),
                request.targetDiscipline(),
                request.primaryGoal(),
                request.secondaryGoals() == null ? Set.of() : request.secondaryGoals(),
                toGrade(request.targetGradeMin()),
                toGrade(request.targetGradeMax()),
                request.durationWeeks(),
                request.sessionsPerWeek(),
                request.avgSessionDurationMinutes(),
                request.trainingVolume(),
                request.requiresHangboard(),
                request.requiresCampusBoard(),
                request.requiresGymAccess(),
                request.requiresOutdoorClimbing(),
                request.recoveryFocused(),
                toEquipment(request.equipment()),
                toWeeks(request.weeks()),
                toTranslations(request.translations())));
        return ResponseEntity.ok(TrainingPlanResponse.from(updated, resolveLocale(acceptLanguage)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete one of the caller's training plans")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        deleteUseCase.delete(id, caller.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/fork")
    @Operation(summary = "Fork a visible training plan as a new PRIVATE copy owned by the caller")
    public ResponseEntity<TrainingPlanResponse> fork(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        TrainingPlan copy = forkUseCase.fork(id, caller.getId());
        return ResponseEntity
                .created(URI.create("/api/v1/training-plans/" + copy.getId()))
                .body(TrainingPlanResponse.from(copy, resolveLocale(acceptLanguage)));
    }

    // ---------------------------------------------------------------------
    // Mapping helpers
    // ---------------------------------------------------------------------

    private static ClimbingGrade toGrade(ClimbingGradeDto dto) {
        if (dto == null) return null;
        try {
            return new ClimbingGrade(GradeScale.valueOf(dto.scale()), dto.value());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid grade scale: " + dto.scale());
        }
    }

    private static List<EquipmentRequirementInput> toEquipment(List<EquipmentRequirementRequest> in) {
        if (in == null) return List.of();
        return in.stream()
                .map(e -> new EquipmentRequirementInput(e.code(), e.optional()))
                .toList();
    }

    private static List<TrainingPlanWeekInput> toWeeks(List<TrainingPlanWeekRequest> in) {
        if (in == null) return List.of();
        return in.stream()
                .map(w -> new TrainingPlanWeekInput(
                        w.weekNumber(),
                        w.deload(),
                        toSessions(w.sessions()),
                        toWeekTranslations(w.translations())))
                .toList();
    }

    private static List<TrainingPlanSessionInput> toSessions(List<TrainingPlanSessionRequest> in) {
        if (in == null) return List.of();
        return in.stream()
                .map(s -> new TrainingPlanSessionInput(
                        s.dayOfWeek(),
                        s.position(),
                        s.workoutTemplateId(),
                        s.optional(),
                        toSessionNotes(s.notesTranslations())))
                .toList();
    }

    private static List<Translation> toTranslations(List<TrainingPlanTranslationDto> in) {
        if (in == null) return List.of();
        return in.stream()
                .map(d -> new Translation(d.locale(), d.field(), d.value()))
                .collect(Collectors.toList());
    }

    private static List<Translation> toWeekTranslations(List<TrainingPlanWeekTranslationDto> in) {
        if (in == null) return List.of();
        return in.stream()
                .map(d -> new Translation(d.locale(), d.field(), d.value()))
                .collect(Collectors.toList());
    }

    private static List<Translation> toSessionNotes(List<TrainingPlanSessionNotesDto> in) {
        if (in == null) return List.of();
        return in.stream()
                .map(d -> new Translation(d.locale(), d.field(), d.value()))
                .collect(Collectors.toList());
    }

    private static String resolveLocale(String header) {
        if (header == null || header.isBlank()) return "es";
        try {
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(header);
            if (ranges.isEmpty()) return "es";
            String tag = ranges.get(0).getRange();
            int dash = tag.indexOf('-');
            return (dash > 0 ? tag.substring(0, dash) : tag).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return "es";
        }
    }
}
