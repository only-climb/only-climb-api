package app.onlyclimb.api.domain.model;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.PlatformContentImmutableException;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregate root for a single training exercise.
 *
 * <p>Exercises come in two flavours:</p>
 * <ul>
 *   <li>{@link ContentSource#PLATFORM} content is curated by the platform admin,
 *       has no owner and is implicitly {@link ContentVisibility#PUBLIC}.
 *       It is immutable once seeded: any update/delete attempt throws
 *       {@link PlatformContentImmutableException}.</li>
 *   <li>{@link ContentSource#USER_CREATED} content always belongs to an owner.
 *       The owner can mutate it and switch visibility between
 *       {@link ContentVisibility#PRIVATE} (default) and
 *       {@link ContentVisibility#PUBLIC}.</li>
 * </ul>
 *
 * <p>Translatable fields ({@code name}, {@code short_description},
 * {@code description}) live in an inner translations map keyed by
 * {@code (locale, field)}. The {@code name} field is required in at least
 * one locale.</p>
 */
public class Exercise {

    /** Whitelisted translatable fields for this aggregate. */
    public static final String FIELD_NAME = "name";
    public static final String FIELD_SHORT_DESCRIPTION = "short_description";
    public static final String FIELD_DESCRIPTION = "description";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            FIELD_NAME, FIELD_SHORT_DESCRIPTION, FIELD_DESCRIPTION);

    private final UUID id;
    private final ContentSource source;
    private final UUID ownerId;
    private String categoryCode;
    private String primaryMuscleGroupCode;
    private ContentVisibility visibility;
    private DifficultyLevel difficultyLevel;
    private SafetyWarningLevel safetyWarningLevel;
    private boolean requiresEquipment;
    private boolean isUnilateral;
    private Integer estimatedDurationMinutes;
    private final EnumSet<ParameterType> allowedParameters;
    /** key = locale + ":" + field */
    private final Map<String, Translation> translations;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public Exercise(
            UUID id,
            ContentSource source,
            UUID ownerId,
            String categoryCode,
            String primaryMuscleGroupCode,
            ContentVisibility visibility,
            DifficultyLevel difficultyLevel,
            SafetyWarningLevel safetyWarningLevel,
            boolean requiresEquipment,
            boolean isUnilateral,
            Integer estimatedDurationMinutes,
            Set<ParameterType> allowedParameters,
            Iterable<Translation> translations,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.source = Objects.requireNonNull(source, "source is required");
        this.visibility = Objects.requireNonNull(visibility, "visibility is required");
        this.difficultyLevel = Objects.requireNonNull(difficultyLevel, "difficultyLevel is required");
        this.safetyWarningLevel = Objects.requireNonNull(safetyWarningLevel, "safetyWarningLevel is required");
        this.categoryCode = requireCode(categoryCode, "categoryCode");
        this.primaryMuscleGroupCode = requireCode(primaryMuscleGroupCode, "primaryMuscleGroupCode");
        this.requiresEquipment = requiresEquipment;
        this.isUnilateral = isUnilateral;
        if (estimatedDurationMinutes != null && estimatedDurationMinutes <= 0) {
            throw new IllegalArgumentException("estimatedDurationMinutes must be positive");
        }
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.allowedParameters = (allowedParameters == null || allowedParameters.isEmpty())
                ? EnumSet.noneOf(ParameterType.class)
                : EnumSet.copyOf(allowedParameters);
        this.translations = new LinkedHashMap<>();
        if (translations != null) {
            for (Translation t : translations) {
                ensureFieldAllowed(t.field());
                this.translations.put(key(t.locale(), t.field()), t);
            }
        }
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.deletedAt = deletedAt;

        if (source == ContentSource.PLATFORM) {
            if (ownerId != null) {
                throw new IllegalArgumentException("PLATFORM exercises cannot have an owner");
            }
            if (visibility != ContentVisibility.PUBLIC) {
                throw new IllegalArgumentException("PLATFORM exercises must be PUBLIC");
            }
        } else if (ownerId == null) {
            throw new IllegalArgumentException("USER_CREATED exercises require an owner");
        }
        this.ownerId = ownerId;

        if (!hasTranslationForField(FIELD_NAME)) {
            throw new IllegalArgumentException("Exercise requires a name translation in at least one locale");
        }
    }

    /** Factory used when a user creates a new exercise. */
    public static Exercise createUserExercise(
            UUID ownerId,
            String categoryCode,
            String primaryMuscleGroupCode,
            DifficultyLevel difficultyLevel,
            SafetyWarningLevel safetyWarningLevel,
            boolean requiresEquipment,
            boolean isUnilateral,
            Integer estimatedDurationMinutes,
            Set<ParameterType> allowedParameters,
            Iterable<Translation> translations,
            ContentVisibility visibility) {
        Instant now = Instant.now();
        return new Exercise(
                UUID.randomUUID(),
                ContentSource.USER_CREATED,
                Objects.requireNonNull(ownerId, "ownerId is required"),
                categoryCode,
                primaryMuscleGroupCode,
                visibility == null ? ContentVisibility.PRIVATE : visibility,
                difficultyLevel,
                safetyWarningLevel,
                requiresEquipment,
                isUnilateral,
                estimatedDurationMinutes,
                allowedParameters,
                translations,
                now,
                now,
                null);
    }

    // ---------------------------------------------------------------------
    // Mutators
    // ---------------------------------------------------------------------

    public void updateDetails(
            String categoryCode,
            String primaryMuscleGroupCode,
            DifficultyLevel difficultyLevel,
            SafetyWarningLevel safetyWarningLevel,
            boolean requiresEquipment,
            boolean isUnilateral,
            Integer estimatedDurationMinutes,
            Set<ParameterType> allowedParameters) {
        requireMutable();
        if (estimatedDurationMinutes != null && estimatedDurationMinutes <= 0) {
            throw new IllegalArgumentException("estimatedDurationMinutes must be positive");
        }
        this.categoryCode = requireCode(categoryCode, "categoryCode");
        this.primaryMuscleGroupCode = requireCode(primaryMuscleGroupCode, "primaryMuscleGroupCode");
        this.difficultyLevel = Objects.requireNonNull(difficultyLevel, "difficultyLevel is required");
        this.safetyWarningLevel = Objects.requireNonNull(safetyWarningLevel, "safetyWarningLevel is required");
        this.requiresEquipment = requiresEquipment;
        this.isUnilateral = isUnilateral;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.allowedParameters.clear();
        if (allowedParameters != null) {
            this.allowedParameters.addAll(allowedParameters);
        }
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
            throw new IllegalArgumentException("Exercise requires a name translation in at least one locale");
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

    /** Ensures the caller can edit this exercise. Owner-only for USER_CREATED. */
    public void assertEditableBy(UUID userId) {
        requireMutable();
        if (!Objects.equals(this.ownerId, userId)) {
            throw new ContentOwnershipException("Caller is not the owner of this exercise");
        }
    }

    // ---------------------------------------------------------------------
    // Read accessors
    // ---------------------------------------------------------------------

    public UUID getId() { return id; }
    public ContentSource getSource() { return source; }
    public Optional<UUID> getOwnerId() { return Optional.ofNullable(ownerId); }
    public String getCategoryCode() { return categoryCode; }
    public String getPrimaryMuscleGroupCode() { return primaryMuscleGroupCode; }
    public ContentVisibility getVisibility() { return visibility; }
    public DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public SafetyWarningLevel getSafetyWarningLevel() { return safetyWarningLevel; }
    public boolean isRequiresEquipment() { return requiresEquipment; }
    public boolean isUnilateral() { return isUnilateral; }
    public Optional<Integer> getEstimatedDurationMinutes() { return Optional.ofNullable(estimatedDurationMinutes); }
    public Set<ParameterType> getAllowedParameters() {
        return Collections.unmodifiableSet(allowedParameters.clone());
    }
    public Set<Translation> getTranslations() {
        return Set.copyOf(translations.values());
    }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Optional<Instant> getDeletedAt() { return Optional.ofNullable(deletedAt); }

    /**
     * Resolves the value of a translatable field for the requested locale.
     * Falls back to {@code es} (project default); if that is missing, falls
     * back to any available translation. Returns empty if the field has no
     * translations.
     */
    public Optional<String> resolveField(String field, String preferredLocale) {
        ensureFieldAllowed(field);
        String locale = preferredLocale == null ? "" : preferredLocale.toLowerCase(Locale.ROOT);
        Translation primary = translations.get(key(locale, field));
        if (primary != null) {
            return Optional.of(primary.value());
        }
        Translation fallback = translations.get(key("es", field));
        if (fallback != null) {
            return Optional.of(fallback.value());
        }
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
                    "PLATFORM exercises cannot be modified");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private boolean hasTranslationForField(String field) {
        for (Translation t : translations.values()) {
            if (t.field().equals(field)) return true;
        }
        return false;
    }

    private static String requireCode(String code, String name) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (code.length() > 50) {
            throw new IllegalArgumentException(name + " exceeds 50 characters");
        }
        return code;
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
