package app.onlyclimb.api.infrastructure.adapter.in.web.workout;

import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.model.WorkoutTemplate;
import app.onlyclimb.api.domain.port.in.CreateWorkoutTemplateCommand;
import app.onlyclimb.api.domain.port.in.CreateWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.DeleteWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.ForkWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.GetWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.ListWorkoutTemplatesQuery;
import app.onlyclimb.api.domain.port.in.ListWorkoutTemplatesUseCase;
import app.onlyclimb.api.domain.port.in.UpdateWorkoutTemplateCommand;
import app.onlyclimb.api.domain.port.in.UpdateWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.WorkoutTemplateExerciseEntry;
import app.onlyclimb.api.domain.port.out.WorkoutTemplateRepository.Page;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.CurrentUserService;
import app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto.CreateWorkoutTemplateRequest;
import app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto.NotesTranslationDto;
import app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto.TranslationDto;
import app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto.UpdateWorkoutTemplateRequest;
import app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto.WorkoutTemplateExerciseRequest;
import app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto.WorkoutTemplatePageResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto.WorkoutTemplateResponse;
import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.DifficultyLevel;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workout-templates")
@RequiredArgsConstructor
@Tag(name = "Workout templates", description = "Platform & user-created workout sessions")
public class WorkoutTemplateController {

    private final CreateWorkoutTemplateUseCase createUseCase;
    private final UpdateWorkoutTemplateUseCase updateUseCase;
    private final DeleteWorkoutTemplateUseCase deleteUseCase;
    private final GetWorkoutTemplateUseCase getUseCase;
    private final ListWorkoutTemplatesUseCase listUseCase;
    private final ForkWorkoutTemplateUseCase forkUseCase;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "List workout templates visible to the caller")
    public ResponseEntity<WorkoutTemplatePageResponse> list(
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) ClimbingDiscipline discipline,
            @RequestParam(required = false) String search,
            @RequestParam(name = "owned", defaultValue = "false") boolean ownedOnly,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        UUID callerId = currentUserService.optionalCurrentId(authentication).orElse(null);
        Page<WorkoutTemplate> page = listUseCase.list(new ListWorkoutTemplatesQuery(
                callerId, difficulty, discipline, search, ownedOnly, cursor, limit));
        String locale = resolveLocale(acceptLanguage);
        List<WorkoutTemplateResponse> data = page.items().stream()
                .map(t -> WorkoutTemplateResponse.from(t, locale))
                .toList();
        return ResponseEntity.ok(new WorkoutTemplatePageResponse(data, page.nextCursor()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a template by UUID")
    public ResponseEntity<WorkoutTemplateResponse> getById(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        UUID callerId = currentUserService.optionalCurrentId(authentication).orElse(null);
        WorkoutTemplate template = getUseCase.getVisible(id, callerId);
        return ResponseEntity.ok(WorkoutTemplateResponse.from(template, resolveLocale(acceptLanguage)));
    }

    @PostMapping
    @Operation(summary = "Create a new user-authored workout template")
    public ResponseEntity<WorkoutTemplateResponse> create(
            @Valid @RequestBody CreateWorkoutTemplateRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        WorkoutTemplate created = createUseCase.create(new CreateWorkoutTemplateCommand(
                caller.getId(),
                request.visibility(),
                request.difficultyLevel(),
                request.estimatedDurationMinutes(),
                request.targetDiscipline(),
                toDomainEntries(request.exercises()),
                toDomainTranslations(request.translations())));
        return ResponseEntity
                .created(URI.create("/api/v1/workout-templates/" + created.getId()))
                .body(WorkoutTemplateResponse.from(created, resolveLocale(acceptLanguage)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of the caller's workout templates")
    public ResponseEntity<WorkoutTemplateResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkoutTemplateRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        WorkoutTemplate updated = updateUseCase.update(new UpdateWorkoutTemplateCommand(
                id,
                caller.getId(),
                request.visibility(),
                request.difficultyLevel(),
                request.estimatedDurationMinutes(),
                request.targetDiscipline(),
                toDomainEntries(request.exercises()),
                toDomainTranslations(request.translations())));
        return ResponseEntity.ok(WorkoutTemplateResponse.from(updated, resolveLocale(acceptLanguage)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete one of the caller's workout templates")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        deleteUseCase.delete(id, caller.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/fork")
    @Operation(summary = "Fork a visible template as a new PRIVATE copy owned by the caller")
    public ResponseEntity<WorkoutTemplateResponse> fork(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        WorkoutTemplate copy = forkUseCase.fork(id, caller.getId());
        return ResponseEntity
                .created(URI.create("/api/v1/workout-templates/" + copy.getId()))
                .body(WorkoutTemplateResponse.from(copy, resolveLocale(acceptLanguage)));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static List<WorkoutTemplateExerciseEntry> toDomainEntries(
            List<WorkoutTemplateExerciseRequest> input) {
        if (input == null) return List.of();
        return input.stream()
                .map(r -> new WorkoutTemplateExerciseEntry(
                        r.position(),
                        r.exerciseId(),
                        r.config() == null ? java.util.Map.of() : r.config(),
                        toDomainNotes(r.notesTranslations())))
                .toList();
    }

    private static List<Translation> toDomainNotes(List<NotesTranslationDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(d -> new Translation(d.locale(), d.field(), d.value()))
                .toList();
    }

    private static List<Translation> toDomainTranslations(List<TranslationDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(d -> new Translation(d.locale(), d.field(), d.value()))
                .toList();
    }

    private static String resolveLocale(String header) {
        if (header == null || header.isBlank()) return "en";
        try {
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(header);
            if (ranges.isEmpty()) return "en";
            String tag = ranges.get(0).getRange();
            int dash = tag.indexOf('-');
            return (dash > 0 ? tag.substring(0, dash) : tag).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return "en";
        }
    }
}
