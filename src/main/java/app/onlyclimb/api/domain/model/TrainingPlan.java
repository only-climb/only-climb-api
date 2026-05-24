package app.onlyclimb.api.domain.model;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.PlatformContentImmutableException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregate root for a multi-week training program. Schedules
 * {@link WorkoutTemplate}s across days and weeks toward a stated
 * {@link GoalType primary goal}.
 *
 * <p>Same source/visibility rules as {@link WorkoutTemplate}:</p>
 * <ul>
 *   <li>{@link ContentSource#PLATFORM} plans are ownerless, PUBLIC and
 *       immutable.</li>
 *   <li>{@link ContentSource#USER_CREATED} plans require an owner and may
 *       toggle visibility between PRIVATE (default) and PUBLIC.</li>
 * </ul>
 *
 * <p>Fork invariant: a fork is a deep, independent copy as a new USER_CREATED
 * plan. Changes to the source never propagate.</p>
 *
 * <p>Translatable fields: {@value #FIELD_NAME}, {@value #FIELD_SHORT_DESCRIPTION},
 * {@value #FIELD_DESCRIPTION}, {@value #FIELD_METHODOLOGY},
 * {@value #FIELD_PREREQUISITES}, {@value #FIELD_EXPECTED_OUTCOMES},
 * {@value #FIELD_AUTHOR_NOTES}, {@value #FIELD_COACHING_TIPS}.</p>
 */
public class TrainingPlan {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_SHORT_DESCRIPTION = "short_description";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_METHODOLOGY = "methodology";
    public static final String FIELD_PREREQUISITES = "prerequisites";
    public static final String FIELD_EXPECTED_OUTCOMES = "expected_outcomes";
    public static final String FIELD_AUTHOR_NOTES = "author_notes";
    public static final String FIELD_COACHING_TIPS = "coaching_tips";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            FIELD_NAME, FIELD_SHORT_DESCRIPTION, FIELD_DESCRIPTION, FIELD_METHODOLOGY,
            FIELD_PREREQUISITES, FIELD_EXPECTED_OUTCOMES, FIELD_AUTHOR_NOTES, FIELD_COACHING_TIPS);

    private final UUID id;
    private final ContentSource source;
    private final PlanGenerationType generationType;
    private final UUID ownerId;
    private final UUID forkedFromId;
    private ContentVisibility visibility;
    private DifficultyLevel difficultyLevel;
    private ClimbingDiscipline targetDiscipline;
    private GoalType primaryGoal;
    private final Set<GoalType> secondaryGoals;
    private ClimbingGrade targetGradeMin;
    private ClimbingGrade targetGradeMax;
    private int durationWeeks;
    private int sessionsPerWeek;
    private Integer avgSessionDurationMinutes;
    private TrainingVolume trainingVolume;
    private boolean requiresHangboard;
    private boolean requiresCampusBoard;
    private boolean requiresGymAccess;
    private boolean requiresOutdoorClimbing;
    private boolean recoveryFocused;
    private final Set<EquipmentRequirement> equipment;
    private final List<TrainingPlanWeek> weeks;
    private final Map<String, Translation> translations;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public TrainingPlan(
            UUID id,
            ContentSource source,
            PlanGenerationType generationType,
            UUID ownerId,
            UUID forkedFromId,
            ContentVisibility visibility,
            DifficultyLevel difficultyLevel,
            ClimbingDiscipline targetDiscipline,
            GoalType primaryGoal,
            Set<GoalType> secondaryGoals,
            ClimbingGrade targetGradeMin,
            ClimbingGrade targetGradeMax,
            int durationWeeks,
            int sessionsPerWeek,
            Integer avgSessionDurationMinutes,
            TrainingVolume trainingVolume,
            boolean requiresHangboard,
            boolean requiresCampusBoard,
            boolean requiresGymAccess,
            boolean requiresOutdoorClimbing,
            boolean recoveryFocused,
            Set<EquipmentRequirement> equipment,
            List<TrainingPlanWeek> weeks,
            Iterable<Translation> translations,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.source = Objects.requireNonNull(source, "source is required");
        this.generationType = Objects.requireNonNull(generationType, "generationType is required");
        this.visibility = Objects.requireNonNull(visibility, "visibility is required");
        this.difficultyLevel = Objects.requireNonNull(difficultyLevel, "difficultyLevel is required");
        this.targetDiscipline = Objects.requireNonNull(targetDiscipline, "targetDiscipline is required");
        this.primaryGoal = Objects.requireNonNull(primaryGoal, "primaryGoal is required");
        this.trainingVolume = trainingVolume == null ? TrainingVolume.MODERATE : trainingVolume;

        if (durationWeeks < 1 || durationWeeks > 104) {
            throw new IllegalArgumentException("durationWeeks must be in [1..104]");
        }
        if (sessionsPerWeek < 1 || sessionsPerWeek > 14) {
            throw new IllegalArgumentException("sessionsPerWeek must be in [1..14]");
        }
        if (avgSessionDurationMinutes != null
                && (avgSessionDurationMinutes < 5 || avgSessionDurationMinutes > 600)) {
            throw new IllegalArgumentException("avgSessionDurationMinutes must be in [5..600]");
        }
        this.durationWeeks = durationWeeks;
        this.sessionsPerWeek = sessionsPerWeek;
        this.avgSessionDurationMinutes = avgSessionDurationMinutes;

        if (source == ContentSource.PLATFORM) {
            if (ownerId != null) {
                throw new IllegalArgumentException("PLATFORM plans cannot have an owner");
            }
            if (visibility != ContentVisibility.PUBLIC) {
                throw new IllegalArgumentException("PLATFORM plans must be PUBLIC");
            }
            if (forkedFromId != null) {
                throw new IllegalArgumentException("PLATFORM plans cannot be forks");
            }
        } else if (ownerId == null) {
            throw new IllegalArgumentException("USER_CREATED plans require an owner");
        }
        this.ownerId = ownerId;
        this.forkedFromId = forkedFromId;

        this.secondaryGoals = EnumSet.noneOf(GoalType.class);
        if (secondaryGoals != null) {
            for (GoalType g : secondaryGoals) {
                if (g == primaryGoal) {
                    throw new IllegalArgumentException(
                            "Secondary goal cannot duplicate the primary goal: " + g);
                }
                this.secondaryGoals.add(g);
            }
        }

        assignGradeRange(targetGradeMin, targetGradeMax);

        this.equipment = new LinkedHashSet<>();
        if (equipment != null) {
            Set<String> seen = new HashSet<>();
            for (EquipmentRequirement e : equipment) {
                if (!seen.add(e.code())) {
                    throw new IllegalArgumentException(
                            "Duplicate equipment code: " + e.code());
                }
                this.equipment.add(e);
            }
        }

        this.weeks = new ArrayList<>();
        if (weeks != null) this.weeks.addAll(weeks);
        validateAndRenumberWeeks();

        this.translations = new LinkedHashMap<>();
        if (translations != null) {
            for (Translation t : translations) {
                ensureFieldAllowed(t.field());
                this.translations.put(key(t.locale(), t.field()), t);
            }
        }
        if (!hasTranslationForField(FIELD_NAME)) {
            throw new IllegalArgumentException(
                    "Plan requires a name translation in at least one locale");
        }

        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.deletedAt = deletedAt;
    }

    /** Factory for a user-authored plan ({@link PlanGenerationType#MANUAL}). */
    public static TrainingPlan createUserPlan(
            UUID ownerId,
            ContentVisibility visibility,
            DifficultyLevel difficultyLevel,
            ClimbingDiscipline targetDiscipline,
            GoalType primaryGoal,
            Set<GoalType> secondaryGoals,
            ClimbingGrade targetGradeMin,
            ClimbingGrade targetGradeMax,
            int durationWeeks,
            int sessionsPerWeek,
            Integer avgSessionDurationMinutes,
            TrainingVolume trainingVolume,
            boolean requiresHangboard,
            boolean requiresCampusBoard,
            boolean requiresGymAccess,
            boolean requiresOutdoorClimbing,
            boolean recoveryFocused,
            Set<EquipmentRequirement> equipment,
            List<TrainingPlanWeek> weeks,
            Iterable<Translation> translations) {
        Instant now = Instant.now();
        return new TrainingPlan(
                UUID.randomUUID(),
                ContentSource.USER_CREATED,
                PlanGenerationType.MANUAL,
                Objects.requireNonNull(ownerId, "ownerId is required"),
                null,
                visibility == null ? ContentVisibility.PRIVATE : visibility,
                difficultyLevel,
                targetDiscipline,
                primaryGoal,
                secondaryGoals,
                targetGradeMin,
                targetGradeMax,
                durationWeeks,
                sessionsPerWeek,
                avgSessionDurationMinutes,
                trainingVolume,
                requiresHangboard,
                requiresCampusBoard,
                requiresGymAccess,
                requiresOutdoorClimbing,
                recoveryFocused,
                equipment,
                weeks,
                translations,
                now, now, null);
    }

    /**
     * Deep-copies this plan as a brand new USER_CREATED plan owned by
     * {@code newOwnerId}. {@code generationType} becomes
     * {@link PlanGenerationType#FORKED} and {@code forkedFromId} is set to this
     * plan's id.
     */
    public TrainingPlan fork(UUID newOwnerId) {
        Objects.requireNonNull(newOwnerId, "newOwnerId is required");
        List<TrainingPlanWeek> weekCopies = new ArrayList<>(weeks.size());
        for (TrainingPlanWeek w : weeks) weekCopies.add(w.copy());
        Instant now = Instant.now();
        return new TrainingPlan(
                UUID.randomUUID(),
                ContentSource.USER_CREATED,
                PlanGenerationType.FORKED,
                newOwnerId,
                this.id,
                ContentVisibility.PRIVATE,
                this.difficultyLevel,
                this.targetDiscipline,
                this.primaryGoal,
                EnumSet.copyOf(this.secondaryGoals.isEmpty()
                        ? EnumSet.noneOf(GoalType.class) : this.secondaryGoals),
                this.targetGradeMin,
                this.targetGradeMax,
                this.durationWeeks,
                this.sessionsPerWeek,
                this.avgSessionDurationMinutes,
                this.trainingVolume,
                this.requiresHangboard,
                this.requiresCampusBoard,
                this.requiresGymAccess,
                this.requiresOutdoorClimbing,
                this.recoveryFocused,
                new LinkedHashSet<>(this.equipment),
                weekCopies,
                this.translations.values(),
                now, now, null);
    }

    // ---------------------------------------------------------------------
    // Mutators
    // ---------------------------------------------------------------------

    public void updateDetails(
            DifficultyLevel difficultyLevel,
            ClimbingDiscipline targetDiscipline,
            GoalType primaryGoal,
            ClimbingGrade targetGradeMin,
            ClimbingGrade targetGradeMax,
            int durationWeeks,
            int sessionsPerWeek,
            Integer avgSessionDurationMinutes,
            TrainingVolume trainingVolume,
            boolean requiresHangboard,
            boolean requiresCampusBoard,
            boolean requiresGymAccess,
            boolean requiresOutdoorClimbing,
            boolean recoveryFocused) {
        requireMutable();
        if (durationWeeks < 1 || durationWeeks > 104) {
            throw new IllegalArgumentException("durationWeeks must be in [1..104]");
        }
        if (sessionsPerWeek < 1 || sessionsPerWeek > 14) {
            throw new IllegalArgumentException("sessionsPerWeek must be in [1..14]");
        }
        if (avgSessionDurationMinutes != null
                && (avgSessionDurationMinutes < 5 || avgSessionDurationMinutes > 600)) {
            throw new IllegalArgumentException("avgSessionDurationMinutes must be in [5..600]");
        }
        this.difficultyLevel = Objects.requireNonNull(difficultyLevel, "difficultyLevel is required");
        this.targetDiscipline = Objects.requireNonNull(targetDiscipline, "targetDiscipline is required");
        GoalType newPrimary = Objects.requireNonNull(primaryGoal, "primaryGoal is required");
        if (this.secondaryGoals.contains(newPrimary)) {
            throw new IllegalArgumentException(
                    "Primary goal cannot duplicate a secondary goal: " + newPrimary);
        }
        this.primaryGoal = newPrimary;
        assignGradeRange(targetGradeMin, targetGradeMax);
        this.durationWeeks = durationWeeks;
        this.sessionsPerWeek = sessionsPerWeek;
        this.avgSessionDurationMinutes = avgSessionDurationMinutes;
        this.trainingVolume = trainingVolume == null ? TrainingVolume.MODERATE : trainingVolume;
        this.requiresHangboard = requiresHangboard;
        this.requiresCampusBoard = requiresCampusBoard;
        this.requiresGymAccess = requiresGymAccess;
        this.requiresOutdoorClimbing = requiresOutdoorClimbing;
        this.recoveryFocused = recoveryFocused;
        touch();
    }

    public void replaceSecondaryGoals(Set<GoalType> newSecondaryGoals) {
        requireMutable();
        this.secondaryGoals.clear();
        if (newSecondaryGoals != null) {
            for (GoalType g : newSecondaryGoals) {
                if (g == primaryGoal) {
                    throw new IllegalArgumentException(
                            "Secondary goal cannot duplicate the primary goal: " + g);
                }
                this.secondaryGoals.add(g);
            }
        }
        touch();
    }

    public void replaceEquipment(Set<EquipmentRequirement> newEquipment) {
        requireMutable();
        this.equipment.clear();
        if (newEquipment != null) {
            Set<String> seen = new HashSet<>();
            for (EquipmentRequirement e : newEquipment) {
                if (!seen.add(e.code())) {
                    throw new IllegalArgumentException(
                            "Duplicate equipment code: " + e.code());
                }
                this.equipment.add(e);
            }
        }
        touch();
    }

    public void replaceWeeks(List<TrainingPlanWeek> newWeeks) {
        requireMutable();
        this.weeks.clear();
        if (newWeeks != null) this.weeks.addAll(newWeeks);
        validateAndRenumberWeeks();
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
                    "Plan requires a name translation in at least one locale");
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
            throw new ContentOwnershipException("Caller is not the owner of this plan");
        }
    }

    // ---------------------------------------------------------------------
    // Read accessors
    // ---------------------------------------------------------------------

    public UUID getId() { return id; }
    public ContentSource getSource() { return source; }
    public PlanGenerationType getGenerationType() { return generationType; }
    public Optional<UUID> getOwnerId() { return Optional.ofNullable(ownerId); }
    public Optional<UUID> getForkedFromId() { return Optional.ofNullable(forkedFromId); }
    public ContentVisibility getVisibility() { return visibility; }
    public DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public ClimbingDiscipline getTargetDiscipline() { return targetDiscipline; }
    public GoalType getPrimaryGoal() { return primaryGoal; }
    public Set<GoalType> getSecondaryGoals() {
        return secondaryGoals.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(secondaryGoals));
    }
    public Optional<ClimbingGrade> getTargetGradeMin() { return Optional.ofNullable(targetGradeMin); }
    public Optional<ClimbingGrade> getTargetGradeMax() { return Optional.ofNullable(targetGradeMax); }
    public int getDurationWeeks() { return durationWeeks; }
    public int getSessionsPerWeek() { return sessionsPerWeek; }
    public Optional<Integer> getAvgSessionDurationMinutes() { return Optional.ofNullable(avgSessionDurationMinutes); }
    public TrainingVolume getTrainingVolume() { return trainingVolume; }
    public boolean isRequiresHangboard() { return requiresHangboard; }
    public boolean isRequiresCampusBoard() { return requiresCampusBoard; }
    public boolean isRequiresGymAccess() { return requiresGymAccess; }
    public boolean isRequiresOutdoorClimbing() { return requiresOutdoorClimbing; }
    public boolean isRecoveryFocused() { return recoveryFocused; }
    public Set<EquipmentRequirement> getEquipment() { return Set.copyOf(equipment); }
    public List<TrainingPlanWeek> getWeeks() { return Collections.unmodifiableList(weeks); }
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
            throw new PlatformContentImmutableException("PLATFORM plans cannot be modified");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private void assignGradeRange(ClimbingGrade min, ClimbingGrade max) {
        GradeScale expectedScale = switch (targetDiscipline) {
            case BOULDER -> GradeScale.FONTAINEBLEAU;
            case SPORT, TRAD -> GradeScale.FRENCH;
        };
        if (min != null && min.getScale() != expectedScale) {
            throw new IllegalArgumentException(
                    "targetGradeMin scale " + min.getScale()
                            + " does not match discipline " + targetDiscipline);
        }
        if (max != null && max.getScale() != expectedScale) {
            throw new IllegalArgumentException(
                    "targetGradeMax scale " + max.getScale()
                            + " does not match discipline " + targetDiscipline);
        }
        this.targetGradeMin = min;
        this.targetGradeMax = max;
    }

    private void validateAndRenumberWeeks() {
        if (weeks.isEmpty()) return;
        Set<Integer> seen = new HashSet<>();
        for (TrainingPlanWeek w : weeks) {
            if (!seen.add(w.getWeekNumber())) {
                throw new IllegalArgumentException(
                        "Duplicate weekNumber " + w.getWeekNumber() + " in plan");
            }
        }
        weeks.sort((a, b) -> Integer.compare(a.getWeekNumber(), b.getWeekNumber()));
        for (int i = 0; i < weeks.size(); i++) {
            weeks.get(i).renumber(i + 1);
        }
        if (weeks.size() > durationWeeks) {
            throw new IllegalArgumentException(
                    "Number of weeks (" + weeks.size()
                            + ") exceeds plan duration (" + durationWeeks + ")");
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
            throw new IllegalArgumentException("Unsupported translatable field: " + field);
        }
    }

    private static String key(String locale, String field) {
        return locale.toLowerCase(Locale.ROOT) + ":" + field;
    }
}
