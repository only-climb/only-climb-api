package app.onlyclimb.api.domain.model;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A single ordered entry inside a {@link WorkoutTemplate}: a reference to an
 * {@link Exercise} together with its planned configuration (sets, reps, weight,
 * etc.) and optional per-locale coach notes.
 *
 * <p>Identity of an entry is its {@code (template, position)} pair — there is
 * no stable surrogate. Entries are deep-copied on fork.</p>
 */
public class WorkoutTemplateExercise {

    public static final String FIELD_NOTES = "notes";

    private int position;
    private final UUID exerciseId;
    private final Map<ParameterType, String> config;
    /** key = locale (single supported field is {@value FIELD_NOTES}). */
    private final Map<String, Translation> notesTranslations;

    public WorkoutTemplateExercise(
            int position,
            UUID exerciseId,
            Map<ParameterType, String> config,
            Iterable<Translation> notesTranslations) {
        if (position <= 0) {
            throw new IllegalArgumentException("position must be positive");
        }
        this.position = position;
        this.exerciseId = Objects.requireNonNull(exerciseId, "exerciseId is required");
        this.config = new LinkedHashMap<>();
        if (config != null) {
            config.forEach((k, v) -> {
                Objects.requireNonNull(k, "config parameter is required");
                if (v == null || v.isBlank()) {
                    throw new IllegalArgumentException(
                            "config value for " + k + " must not be blank");
                }
                this.config.put(k, v);
            });
        }
        this.notesTranslations = new LinkedHashMap<>();
        if (notesTranslations != null) {
            for (Translation t : notesTranslations) {
                if (!FIELD_NOTES.equals(t.field())) {
                    throw new IllegalArgumentException(
                            "Only '" + FIELD_NOTES + "' field is supported on entries");
                }
                this.notesTranslations.put(t.locale().toLowerCase(Locale.ROOT), t);
            }
        }
    }

    /** Deep-copy constructor used by fork. */
    public WorkoutTemplateExercise copy() {
        return new WorkoutTemplateExercise(
                position, exerciseId, new LinkedHashMap<>(config), notesTranslations.values());
    }

    void renumber(int newPosition) {
        if (newPosition <= 0) {
            throw new IllegalArgumentException("position must be positive");
        }
        this.position = newPosition;
    }

    public int getPosition() { return position; }
    public UUID getExerciseId() { return exerciseId; }
    public Map<ParameterType, String> getConfig() { return Map.copyOf(config); }
    public java.util.Set<Translation> getNotesTranslations() {
        return java.util.Set.copyOf(notesTranslations.values());
    }

    public Optional<String> resolveNotes(String preferredLocale) {
        String locale = preferredLocale == null ? "" : preferredLocale.toLowerCase(Locale.ROOT);
        Translation primary = notesTranslations.get(locale);
        if (primary != null) return Optional.of(primary.value());
        Translation english = notesTranslations.get("en");
        if (english != null) return Optional.of(english.value());
        return notesTranslations.values().stream().findFirst().map(Translation::value);
    }
}
