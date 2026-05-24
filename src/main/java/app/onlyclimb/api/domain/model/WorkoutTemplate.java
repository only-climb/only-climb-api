package app.onlyclimb.api.domain.model;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.PlatformContentImmutableException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
 * Aggregate root for a named, ordered collection of exercises — i.e. a
 * training session.
 *
 * <p>Same {@link ContentSource} rules as {@link Exercise}: PLATFORM templates
 * are ownerless, PUBLIC and immutable; USER_CREATED templates require an owner
 * and may toggle visibility between PRIVATE (default) and PUBLIC.</p>
 *
 * <p>A user can {@link #fork forking} a template: the application service
 * orchestrates a deep copy as a new USER_CREATED template; later changes to
 * the source never propagate.</p>
 *
 * <p>Translatable fields: {@code name}, {@code description}, {@code coach_notes}.</p>
 */
public class WorkoutTemplate {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_COACH_NOTES = "coach_notes";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            FIELD_NAME, FIELD_DESCRIPTION, FIELD_COACH_NOTES);

    private final UUID id;
    private final ContentSource source;
    private final UUID ownerId;
    private final UUID forkedFromId;
    private ContentVisibility visibility;
    private DifficultyLevel difficultyLevel;
    private Integer estimatedDurationMinutes;
    private ClimbingDiscipline targetDiscipline;
    private final List<WorkoutTemplateExercise> exercises;
    private final Map<String, Translation> translations;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public WorkoutTemplate(
            UUID id,
            ContentSource source,
            UUID ownerId,
            UUID forkedFromId,
            ContentVisibility visibility,
            DifficultyLevel difficultyLevel,
            Integer estimatedDurationMinutes,
            ClimbingDiscipline targetDiscipline,
            List<WorkoutTemplateExercise> exercises,
            Iterable<Translation> translations,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.source = Objects.requireNonNull(source, "source is required");
        this.visibility = Objects.requireNonNull(visibility, "visibility is required");
        this.difficultyLevel = Objects.requireNonNull(difficultyLevel, "difficultyLevel is required");
        if (estimatedDurationMinutes != null && estimatedDurationMinutes <= 0) {
            throw new IllegalArgumentException("estimatedDurationMinutes must be positive");
        }
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.targetDiscipline = targetDiscipline;

        if (source == ContentSource.PLATFORM) {
            if (ownerId != null) {
                throw new IllegalArgumentException("PLATFORM templates cannot have an owner");
            }
            if (visibility != ContentVisibility.PUBLIC) {
                throw new IllegalArgumentException("PLATFORM templates must be PUBLIC");
            }
            if (forkedFromId != null) {
                throw new IllegalArgumentException("PLATFORM templates cannot be forks");
            }
        } else if (ownerId == null) {
            throw new IllegalArgumentException("USER_CREATED templates require an owner");
        }
        this.ownerId = ownerId;
        this.forkedFromId = forkedFromId;

        this.exercises = new ArrayList<>();
        if (exercises != null) {
            for (WorkoutTemplateExercise e : exercises) {
                this.exercises.add(e);
            }
        }
        if (this.exercises.isEmpty()) {
            throw new IllegalArgumentException("Template must contain at least one exercise");
        }
        renumberAndValidatePositions();

        this.translations = new LinkedHashMap<>();
        if (translations != null) {
            for (Translation t : translations) {
                ensureFieldAllowed(t.field());
                this.translations.put(key(t.locale(), t.field()), t);
            }
        }
        if (!hasTranslationForField(FIELD_NAME)) {
            throw new IllegalArgumentException(
                    "Template requires a name translation in at least one locale");
        }

        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.deletedAt = deletedAt;
    }

    /** Factory used when a user authors a new template. */
    public static WorkoutTemplate createUserTemplate(
            UUID ownerId,
            ContentVisibility visibility,
            DifficultyLevel difficultyLevel,
            Integer estimatedDurationMinutes,
            ClimbingDiscipline targetDiscipline,
            List<WorkoutTemplateExercise> exercises,
            Iterable<Translation> translations) {
        Instant now = Instant.now();
        return new WorkoutTemplate(
                UUID.randomUUID(),
                ContentSource.USER_CREATED,
                Objects.requireNonNull(ownerId, "ownerId is required"),
                null,
                visibility == null ? ContentVisibility.PRIVATE : visibility,
                difficultyLevel,
                estimatedDurationMinutes,
                targetDiscipline,
                exercises,
                translations,
                now, now, null);
    }

    /**
     * Deep-copies this template as a brand new USER_CREATED template owned by
     * {@code newOwnerId}. Exercises and translations are copied by value;
     * {@code forkedFromId} is set to this template's id.
     */
    public WorkoutTemplate fork(UUID newOwnerId) {
        Objects.requireNonNull(newOwnerId, "newOwnerId is required");
        List<WorkoutTemplateExercise> copies = new ArrayList<>(exercises.size());
        for (WorkoutTemplateExercise e : exercises) {
            copies.add(e.copy());
        }
        Instant now = Instant.now();
        return new WorkoutTemplate(
                UUID.randomUUID(),
                ContentSource.USER_CREATED,
                newOwnerId,
                this.id,
                ContentVisibility.PRIVATE,
                this.difficultyLevel,
                this.estimatedDurationMinutes,
                this.targetDiscipline,
                copies,
                this.translations.values(),
                now, now, null);
    }

    // ---------------------------------------------------------------------
    // Mutators
    // ---------------------------------------------------------------------

    public void updateDetails(
            DifficultyLevel difficultyLevel,
            Integer estimatedDurationMinutes,
            ClimbingDiscipline targetDiscipline) {
        requireMutable();
        if (estimatedDurationMinutes != null && estimatedDurationMinutes <= 0) {
            throw new IllegalArgumentException("estimatedDurationMinutes must be positive");
        }
        this.difficultyLevel = Objects.requireNonNull(difficultyLevel, "difficultyLevel is required");
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.targetDiscipline = targetDiscipline;
        touch();
    }

    public void replaceExercises(List<WorkoutTemplateExercise> newExercises) {
        requireMutable();
        if (newExercises == null || newExercises.isEmpty()) {
            throw new IllegalArgumentException("Template must contain at least one exercise");
        }
        this.exercises.clear();
        this.exercises.addAll(newExercises);
        renumberAndValidatePositions();
        touch();
    }

    public void replaceTranslations(Iterable<Translation> newTranslations) {
        requireMutable();
        Map<String, Translation> staged = new HashMap<>();
        if (newTranslations != null) {
            for (Translation t : newTranslations) {
                ensureFieldAllowed(t.field());
                staged.put(key(t.locale(), t.field()), t);
            }
        }
        boolean hasName = staged.values().stream()
                .anyMatch(t -> FIELD_NAME.equals(t.field()));
        if (!hasName) {
            throw new IllegalArgumentException(
                    "Template requires a name translation in at least one locale");
        }
        this.translations.clear();
        this.translations.putAll(staged);
        touch();
    }

    public void changeVisibility(ContentVisibility newVisibility) {
        requireMutable();
        this.visibility = Objects.requireNonNull(newVisibility, "visibility is required");
        touch();
    }

    public void softDelete() {
        requireMutable();
        if (deletedAt == null) {
            this.deletedAt = Instant.now();
            this.updatedAt = this.deletedAt;
        }
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public void assertEditableBy(UUID userId) {
        requireMutable();
        if (!Objects.equals(this.ownerId, userId)) {
            throw new ContentOwnershipException("Caller is not the owner of this template");
        }
    }

    // ---------------------------------------------------------------------
    // Read accessors
    // ---------------------------------------------------------------------

    public UUID getId() { return id; }
    public ContentSource getSource() { return source; }
    public Optional<UUID> getOwnerId() { return Optional.ofNullable(ownerId); }
    public Optional<UUID> getForkedFromId() { return Optional.ofNullable(forkedFromId); }
    public ContentVisibility getVisibility() { return visibility; }
    public DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public Optional<Integer> getEstimatedDurationMinutes() { return Optional.ofNullable(estimatedDurationMinutes); }
    public Optional<ClimbingDiscipline> getTargetDiscipline() { return Optional.ofNullable(targetDiscipline); }
    public List<WorkoutTemplateExercise> getExercises() { return Collections.unmodifiableList(exercises); }
    public Set<Translation> getTranslations() { return Set.copyOf(translations.values()); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Optional<Instant> getDeletedAt() { return Optional.ofNullable(deletedAt); }

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

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private void requireMutable() {
        if (source == ContentSource.PLATFORM) {
            throw new PlatformContentImmutableException(
                    "PLATFORM templates cannot be modified");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    /**
     * Sorts entries by their declared position and renumbers them contiguously
     * starting at 1. Rejects duplicates.
     */
    private void renumberAndValidatePositions() {
        Set<Integer> seen = new HashSet<>();
        for (WorkoutTemplateExercise e : exercises) {
            if (!seen.add(e.getPosition())) {
                throw new IllegalArgumentException(
                        "Duplicate position " + e.getPosition() + " in template");
            }
        }
        exercises.sort((a, b) -> Integer.compare(a.getPosition(), b.getPosition()));
        for (int i = 0; i < exercises.size(); i++) {
            exercises.get(i).renumber(i + 1);
        }
    }

    private boolean hasTranslationForField(String field) {
        for (Translation t : translations.values()) {
            if (t.field().equals(field)) return true;
        }
        return false;
    }

    private static void ensureFieldAllowed(String field) {
        if (!ALLOWED_FIELDS.contains(field)) {
            throw new IllegalArgumentException(
                    "Unsupported translatable field: " + field);
        }
    }

    private static String key(String locale, String field) {
        return locale.toLowerCase(Locale.ROOT) + ":" + field;
    }
}
