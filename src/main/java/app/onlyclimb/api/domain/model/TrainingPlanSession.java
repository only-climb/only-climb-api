package app.onlyclimb.api.domain.model;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A single planned session inside a {@link TrainingPlanWeek}. Points to the
 * {@link WorkoutTemplate} the climber is expected to follow on a given day.
 *
 * <p>Identity is the {@code (week, day_of_week, position)} tuple — there is no
 * stable surrogate for the slot. The aggregate exposes the session UUID so
 * external references (e.g. {@code workout_logs.plan_session_id}) remain
 * stable across renumbering.</p>
 *
 * <p>Translatable field: {@value #FIELD_NOTES} (per-session coach note).</p>
 */
public class TrainingPlanSession {

    public static final String FIELD_NOTES = "notes";

    private final UUID id;
    private int dayOfWeek;
    private int position;
    private final UUID workoutTemplateId;
    private boolean optional;
    private final Map<String, Translation> notesTranslations;

    public TrainingPlanSession(
            UUID id,
            int dayOfWeek,
            int position,
            UUID workoutTemplateId,
            boolean optional,
            Iterable<Translation> notesTranslations) {
        this.id = Objects.requireNonNull(id, "id is required");
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("dayOfWeek must be in [1..7]");
        }
        if (position < 1) {
            throw new IllegalArgumentException("position must be positive");
        }
        this.dayOfWeek = dayOfWeek;
        this.position = position;
        this.workoutTemplateId = Objects.requireNonNull(workoutTemplateId,
                "workoutTemplateId is required");
        this.optional = optional;
        this.notesTranslations = new LinkedHashMap<>();
        if (notesTranslations != null) {
            for (Translation t : notesTranslations) {
                if (!FIELD_NOTES.equals(t.field())) {
                    throw new IllegalArgumentException(
                            "Only '" + FIELD_NOTES + "' is supported on sessions");
                }
                this.notesTranslations.put(t.locale().toLowerCase(Locale.ROOT), t);
            }
        }
    }

    /** Deep-copy with a fresh UUID — used by fork. */
    public TrainingPlanSession copy() {
        return new TrainingPlanSession(
                UUID.randomUUID(),
                dayOfWeek,
                position,
                workoutTemplateId,
                optional,
                notesTranslations.values());
    }

    void renumber(int newPosition) {
        if (newPosition < 1) {
            throw new IllegalArgumentException("position must be positive");
        }
        this.position = newPosition;
    }

    public UUID getId() { return id; }
    public int getDayOfWeek() { return dayOfWeek; }
    public int getPosition() { return position; }
    public UUID getWorkoutTemplateId() { return workoutTemplateId; }
    public boolean isOptional() { return optional; }

    public java.util.Set<Translation> getNotesTranslations() {
        return java.util.Set.copyOf(notesTranslations.values());
    }

    public Optional<String> resolveNotes(String preferredLocale) {
        String locale = preferredLocale == null ? "" : preferredLocale.toLowerCase(Locale.ROOT);
        Translation primary = notesTranslations.get(locale);
        if (primary != null) return Optional.of(primary.value());
        Translation fallback = notesTranslations.get("es");
        if (fallback != null) return Optional.of(fallback.value());
        return notesTranslations.values().stream().findFirst().map(Translation::value);
    }
}
