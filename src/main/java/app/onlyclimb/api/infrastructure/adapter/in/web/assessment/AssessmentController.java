package app.onlyclimb.api.infrastructure.adapter.in.web.assessment;

import app.onlyclimb.api.domain.model.AssessmentDefinition;
import app.onlyclimb.api.domain.model.AssessmentResult;
import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.port.in.AssessmentDefinitionUseCase;
import app.onlyclimb.api.domain.port.in.DeleteAssessmentResultUseCase;
import app.onlyclimb.api.domain.port.in.GetAssessmentResultUseCase;
import app.onlyclimb.api.domain.port.in.ListAssessmentResultsQuery;
import app.onlyclimb.api.domain.port.in.ListAssessmentResultsUseCase;
import app.onlyclimb.api.domain.port.in.RecordAssessmentResultCommand;
import app.onlyclimb.api.domain.port.in.RecordAssessmentResultUseCase;
import app.onlyclimb.api.domain.port.out.AssessmentResultRepository.Page;
import app.onlyclimb.api.infrastructure.adapter.in.web.assessment.dto.AssessmentDefinitionResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.assessment.dto.AssessmentResultPageResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.assessment.dto.AssessmentResultResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.assessment.dto.RecordAssessmentResultRequest;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.CurrentUserService;
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
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
@Tag(name = "Assessments",
        description = "Platform-curated assessment definitions and user-recorded results")
public class AssessmentController {

    private final AssessmentDefinitionUseCase definitionUseCase;
    private final RecordAssessmentResultUseCase recordUseCase;
    private final ListAssessmentResultsUseCase listUseCase;
    private final GetAssessmentResultUseCase getUseCase;
    private final DeleteAssessmentResultUseCase deleteUseCase;
    private final CurrentUserService currentUserService;

    // -- Definitions ---------------------------------------------------------

    @GetMapping("/definitions")
    @Operation(summary = "List active assessment definitions, optionally filtered by discipline")
    public ResponseEntity<List<AssessmentDefinitionResponse>> listDefinitions(
            @RequestParam(required = false) ClimbingDiscipline discipline,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        String locale = resolveLocale(acceptLanguage);
        List<AssessmentDefinitionResponse> data = definitionUseCase.listActive(discipline).stream()
                .map(d -> AssessmentDefinitionResponse.from(d, locale))
                .toList();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/definitions/{id}")
    @Operation(summary = "Get a single assessment definition by UUID")
    public ResponseEntity<AssessmentDefinitionResponse> getDefinition(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        AssessmentDefinition def = definitionUseCase.getById(id);
        return ResponseEntity.ok(AssessmentDefinitionResponse.from(def, resolveLocale(acceptLanguage)));
    }

    // -- Results -------------------------------------------------------------

    @GetMapping("/results")
    @Operation(summary = "List the caller's assessment results (paginated)")
    public ResponseEntity<AssessmentResultPageResponse> listResults(
            @RequestParam(required = false) UUID definitionId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") int limit,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        Page<AssessmentResult> page = listUseCase.list(new ListAssessmentResultsQuery(
                caller.getId(), definitionId, cursor, limit));
        List<AssessmentResultResponse> data = page.items().stream()
                .map(AssessmentResultResponse::from).toList();
        return ResponseEntity.ok(new AssessmentResultPageResponse(data, page.nextCursor()));
    }

    @GetMapping("/results/{id}")
    @Operation(summary = "Get one of the caller's assessment results by UUID")
    public ResponseEntity<AssessmentResultResponse> getResult(
            @PathVariable UUID id, Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        AssessmentResult result = getUseCase.getOwned(id, caller.getId());
        return ResponseEntity.ok(AssessmentResultResponse.from(result));
    }

    @PostMapping("/results")
    @Operation(summary = "Record a new assessment result for the caller")
    public ResponseEntity<AssessmentResultResponse> recordResult(
            @Valid @RequestBody RecordAssessmentResultRequest request,
            Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        AssessmentResult created = recordUseCase.record(new RecordAssessmentResultCommand(
                caller.getId(),
                request.definitionId(),
                request.performedAt(),
                request.userWeightKg(),
                request.notes(),
                request.metrics().stream().map(m -> m.toInput()).toList()));
        return ResponseEntity
                .created(URI.create("/api/v1/assessments/results/" + created.getId()))
                .body(AssessmentResultResponse.from(created));
    }

    @DeleteMapping("/results/{id}")
    @Operation(summary = "Hard-delete one of the caller's assessment results")
    public ResponseEntity<Void> deleteResult(
            @PathVariable UUID id, Authentication authentication) {
        var caller = currentUserService.requireCurrent(authentication);
        deleteUseCase.delete(id, caller.getId());
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------------

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
