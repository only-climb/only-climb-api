package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.ParameterType;
import app.onlyclimb.api.domain.model.Translation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One ordered exercise inside a template command payload.
 *
 * <p>{@code position} is the desired (1-based) slot; the aggregate will
 * renumber on save. {@code config} keys must be a subset of the referenced
 * exercise's allowed parameters (validated by the application service).
 * {@code notesTranslations} entries must all use the {@code notes} field.</p>
 */
public record WorkoutTemplateExerciseEntry(
        int position,
        UUID exerciseId,
        Map<ParameterType, String> config,
        List<Translation> notesTranslations) {
}
