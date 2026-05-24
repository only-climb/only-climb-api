package app.onlyclimb.api.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * One ordinal week of a {@link TrainingPlan}, containing its planned
 * {@link TrainingPlanSession sessions}.
 *
 * <p>{@code (plan, weekNumber)} must be unique within the plan and consecutive
 * starting at 1 — enforced by {@link TrainingPlan} on construction.</p>
 *
 * <p>A {@link #isDeload() deload} week is a planned reduction in load used for
 * recovery / supercompensation cycles. It still counts as a week of the plan.</p>
 *
 * <p>Translatable fields: {@value #FIELD_NAME}, {@value #FIELD_FOCUS},
 * {@value #FIELD_NOTES}.</p>
 */
public class TrainingPlanWeek {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_FOCUS = "focus";
    public static final String FIELD_NOTES = "notes";

    private static final Set<String> ALLOWED_FIELDS = Set.of(FIELD_NAME, FIELD_FOCUS, FIELD_NOTES);

    private final UUID id;
    private int weekNumber;
    private boolean deload;
    private final List<TrainingPlanSession> sessions;
    private final Map<String, Translation> translations;

    public TrainingPlanWeek(
            UUID id,
            int weekNumber,
            boolean deload,
            List<TrainingPlanSession> sessions,
            Iterable<Translation> translations) {
        this.id = Objects.requireNonNull(id, "id is required");
        if (weekNumber < 1) {
            throw new IllegalArgumentException("weekNumber must be positive");
        }
        this.weekNumber = weekNumber;
        this.deload = deload;
        this.sessions = new ArrayList<>();
        if (sessions != null) {
            this.sessions.addAll(sessions);
        }
        validateSessions();
        this.translations = new LinkedHashMap<>();
        if (translations != null) {
            for (Translation t : translations) {
                ensureFieldAllowed(t.field());
                this.translations.put(key(t.locale(), t.field()), t);
            }
        }
    }

    /** Deep-copy with fresh UUIDs — used by {@link TrainingPlan#fork(UUID)}. */
    public TrainingPlanWeek copy() {
        List<TrainingPlanSession> copies = new ArrayList<>(sessions.size());
        for (TrainingPlanSession s : sessions) {
            copies.add(s.copy());
        }
        return new TrainingPlanWeek(
                UUID.randomUUID(),
                weekNumber,
                deload,
                copies,
                translations.values());
    }

    void renumber(int newWeekNumber) {
        if (newWeekNumber < 1) {
            throw new IllegalArgumentException("weekNumber must be positive");
        }
        this.weekNumber = newWeekNumber;
    }

    public UUID getId() { return id; }
    public int getWeekNumber() { return weekNumber; }
    public boolean isDeload() { return deload; }
    public List<TrainingPlanSession> getSessions() { return Collections.unmodifiableList(sessions); }
    public Set<Translation> getTranslations() { return Set.copyOf(translations.values()); }

    public Optional<String> resolveField(String field, String preferredLocale) {
        ensureFieldAllowed(field);
        String locale = preferredLocale == null ? "" : preferredLocale.toLowerCase(Locale.ROOT);
        Translation primary = translations.get(key(locale, field));
        if (primary != null) return Optional.of(primary.value());
        Translation fallback = translations.get(key("es", field));
        if (fallback != null) return Optional.of(fallback.value());
        return translations.values().stream()
                .filter(t -> t.field().equals(field))
                .findFirst()
                .map(Translation::value);
    }

    private void validateSessions() {
        // (day_of_week, position) must be unique within a week.
        Set<String> seen = new HashSet<>();
        for (TrainingPlanSession s : sessions) {
            String slot = s.getDayOfWeek() + ":" + s.getPosition();
            if (!seen.add(slot)) {
                throw new IllegalArgumentException(
                        "Duplicate session slot day=" + s.getDayOfWeek()
                                + " position=" + s.getPosition() + " in week " + weekNumber);
            }
        }
        sessions.sort((a, b) -> {
            int c = Integer.compare(a.getDayOfWeek(), b.getDayOfWeek());
            return c != 0 ? c : Integer.compare(a.getPosition(), b.getPosition());
        });
    }

    private static void ensureFieldAllowed(String field) {
        if (!ALLOWED_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Unsupported translatable field: " + field);
        }
    }

    private static String key(String locale, String field) {
        return locale.toLowerCase(Locale.ROOT) + ":" + field;
    }
}
