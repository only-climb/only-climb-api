package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.Translation;

import java.util.List;

/**
 * One week inside a training-plan command payload.
 *
 * <p>{@code weekNumber} is the desired (1-based) slot; the aggregate renumbers
 * on save. Translations are constrained to the
 * {@link app.onlyclimb.api.domain.model.TrainingPlanWeek allowed fields}
 * (name, focus, notes).</p>
 */
public record TrainingPlanWeekInput(
        int weekNumber,
        boolean deload,
        List<TrainingPlanSessionInput> sessions,
        List<Translation> translations) {
}
