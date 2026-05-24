package app.onlyclimb.api.infrastructure.adapter.in.web.exercise;

import app.onlyclimb.api.domain.model.Exercise;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.port.in.CreateExerciseCommand;
import app.onlyclimb.api.domain.port.in.CreateExerciseUseCase;
import app.onlyclimb.api.domain.port.in.DeleteExerciseUseCase;
import app.onlyclimb.api.domain.port.in.GetExerciseUseCase;
import app.onlyclimb.api.domain.port.in.ListExercisesQuery;
import app.onlyclimb.api.domain.port.in.ListExercisesUseCase;
import app.onlyclimb.api.domain.port.in.UpdateExerciseCommand;
import app.onlyclimb.api.domain.port.in.UpdateExerciseUseCase;
import app.onlyclimb.api.domain.port.out.ExerciseRepository.Page;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.CurrentUserService;
import app.onlyclimb.api.infrastructure.adapter.in.web.exercise.dto.CreateExerciseRequest;
import app.onlyclimb.api.infrastructure.adapter.in.web.exercise.dto.ExercisePageResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.exercise.dto.ExerciseResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.exercise.dto.TranslationDto;
import app.onlyclimb.api.infrastructure.adapter.in.web.exercise.dto.UpdateExerciseRequest;
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

/**
 * Exercise endpoints.
 *
 * <p>Reads ({@code GET}) are public. Mutations require authentication and are
 * scoped to the owner of the resource (admins are not granted special powers
 * over user-authored content; platform content is immutable from the API).</p>
 */
@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercises", description = "Platform & user-created exercises")
public class ExerciseController {

    private final CreateExerciseUseCase createExerciseUseCase;
    private final UpdateExerciseUseCase updateExerciseUseCase;
    private final DeleteExerciseUseCase deleteExerciseUseCase;
    private final GetExerciseUseCase getExerciseUseCase;
    private final ListExercisesUseCase listExercisesUseCase;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "List exercises visible to the caller")
    public ResponseEntity<ExercisePageResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(name = "owned", defaultValue = "false") boolean ownedOnly,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        UUID callerId = currentUserService.optionalCurrentId(authentication).orElse(null);
        Page<Exercise> page = listExercisesUseCase.list(
                new ListExercisesQuery(callerId, category, search, ownedOnly, cursor, limit));
        String locale = resolveLocale(acceptLanguage);
        List<ExerciseResponse> data = page.items().stream()
                .map(e -> ExerciseResponse.from(e, locale))
                .toList();
        return ResponseEntity.ok(new ExercisePageResponse(data, page.nextCursor()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an exercise by UUID")
    public ResponseEntity<ExerciseResponse> getById(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        UUID callerId = currentUserService.optionalCurrentId(authentication).orElse(null);
        Exercise exercise = getExerciseUseCase.getVisible(id, callerId);
        return ResponseEntity.ok(ExerciseResponse.from(exercise, resolveLocale(acceptLanguage)));
    }

    @PostMapping
    @Operation(summary = "Create a new user-authored exercise")
    public ResponseEntity<ExerciseResponse> create(
            @Valid @RequestBody CreateExerciseRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        Exercise created = createExerciseUseCase.create(new CreateExerciseCommand(
                caller.getId(),
                request.categoryCode(),
                request.primaryMuscleGroupCode(),
                request.difficultyLevel(),
                request.safetyWarningLevel(),
                request.requiresEquipment(),
                request.isUnilateral(),
                request.estimatedDurationMinutes(),
                normalizeParameters(request.allowedParameters()),
                toDomainTranslations(request.translations()),
                request.visibility()));
        return ResponseEntity
                .created(URI.create("/api/v1/exercises/" + created.getId()))
                .body(ExerciseResponse.from(created, resolveLocale(acceptLanguage)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of the caller's exercises")
    public ResponseEntity<ExerciseResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExerciseRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        Exercise updated = updateExerciseUseCase.update(new UpdateExerciseCommand(
                id,
                caller.getId(),
                request.categoryCode(),
                request.primaryMuscleGroupCode(),
                request.difficultyLevel(),
                request.safetyWarningLevel(),
                request.requiresEquipment(),
                request.isUnilateral(),
                request.estimatedDurationMinutes(),
                normalizeParameters(request.allowedParameters()),
                toDomainTranslations(request.translations()),
                request.visibility()));
        return ResponseEntity.ok(ExerciseResponse.from(updated, resolveLocale(acceptLanguage)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete one of the caller's exercises")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        deleteExerciseUseCase.delete(id, caller.getId());
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static List<Translation> toDomainTranslations(List<TranslationDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(d -> new Translation(d.locale(), d.field(), d.value()))
                .toList();
    }

    private static Set<app.onlyclimb.api.domain.model.ParameterType> normalizeParameters(
            Set<app.onlyclimb.api.domain.model.ParameterType> input) {
        return input == null ? Set.of() : input;
    }

    private static String resolveLocale(String header) {
        if (header == null || header.isBlank()) return "en";
        try {
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(header);
            if (ranges.isEmpty()) return "en";
            String tag = ranges.get(0).getRange();
            // We only care about the primary subtag (e.g. "es-ES" → "es").
            int dash = tag.indexOf('-');
            return (dash > 0 ? tag.substring(0, dash) : tag).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return "en";
        }
    }
}
