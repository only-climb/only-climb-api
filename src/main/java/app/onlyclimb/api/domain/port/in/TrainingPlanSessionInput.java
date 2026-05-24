package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.Translation;

import java.util.List;
import java.util.UUID;

/**
 * One planned session inside a {@link TrainingPlanWeekInput week input}.
 *
 * <p>{@code dayOfWeek} is in [1..7] (Mon..Sun). {@code position} disambiguates
 * multiple sessions on the same day — the aggregate enforces uniqueness of
 * {@code (dayOfWeek, position)} per week.</p>
 *
 * <p>{@code workoutTemplateId} must reference an existing template visible to
 * the caller (validated by the application service).</p>
 *
 * <p>{@code notesTranslations}, if any, must all use the {@code notes} field.</p>
 */
public record TrainingPlanSessionInput(
        int dayOfWeek,
        int position,
        UUID workoutTemplateId,
        boolean optional,
        List<Translation> notesTranslations) {
}
