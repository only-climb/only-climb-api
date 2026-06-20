package app.onlyclimb.api.domain.model;

import java.util.Objects;

/**
 * Tier-specific limits for feature gating.
 * Immutable value object — limits are defined in code, not in the database.
 */
public final class FeatureLimits {

    private final int maxTrainingPlans;
    private final int maxWorkoutTemplates;
    private final int maxExercises;
    private final int maxActiveGoals;
    private final int maxWorkoutLogs;
    private final int maxAssessmentResults;
    private final int maxPlatformForks;
    private final int aiGenerationsPerMonth;

    public FeatureLimits(int maxTrainingPlans, int maxWorkoutTemplates, int maxExercises,
                         int maxActiveGoals, int maxWorkoutLogs, int maxAssessmentResults,
                         int maxPlatformForks, int aiGenerationsPerMonth) {
        if (maxTrainingPlans < 0) throw new IllegalArgumentException("maxTrainingPlans must be >= 0");
        if (maxWorkoutTemplates < 0) throw new IllegalArgumentException("maxWorkoutTemplates must be >= 0");
        if (maxExercises < 0) throw new IllegalArgumentException("maxExercises must be >= 0");
        if (maxActiveGoals < 0) throw new IllegalArgumentException("maxActiveGoals must be >= 0");
        if (maxWorkoutLogs < 0) throw new IllegalArgumentException("maxWorkoutLogs must be >= 0");
        if (maxAssessmentResults < 0) throw new IllegalArgumentException("maxAssessmentResults must be >= 0");
        if (maxPlatformForks < 0) throw new IllegalArgumentException("maxPlatformForks must be >= 0");
        if (aiGenerationsPerMonth < 0) throw new IllegalArgumentException("aiGenerationsPerMonth must be >= 0");
        this.maxTrainingPlans = maxTrainingPlans;
        this.maxWorkoutTemplates = maxWorkoutTemplates;
        this.maxExercises = maxExercises;
        this.maxActiveGoals = maxActiveGoals;
        this.maxWorkoutLogs = maxWorkoutLogs;
        this.maxAssessmentResults = maxAssessmentResults;
        this.maxPlatformForks = maxPlatformForks;
        this.aiGenerationsPerMonth = aiGenerationsPerMonth;
    }

    // --- Pre-defined tier limits ---

    public static final FeatureLimits FREE = new FeatureLimits(
            1,    // maxTrainingPlans
            3,    // maxWorkoutTemplates
            5,    // maxExercises
            1,    // maxActiveGoals
            30,   // maxWorkoutLogs
            1,    // maxAssessmentResults
            3,    // maxPlatformForks
            0     // aiGenerationsPerMonth
    );

    public static final FeatureLimits BASIC = new FeatureLimits(
            3,    // maxTrainingPlans
            10,   // maxWorkoutTemplates
            20,   // maxExercises
            3,    // maxActiveGoals
            Integer.MAX_VALUE, // maxWorkoutLogs — unlimited
            10,   // maxAssessmentResults
            Integer.MAX_VALUE, // maxPlatformForks — unlimited
            5     // aiGenerationsPerMonth
    );

    public static final FeatureLimits PREMIUM = new FeatureLimits(
            Integer.MAX_VALUE, // unlimited
            Integer.MAX_VALUE, // unlimited
            Integer.MAX_VALUE, // unlimited
            5,    // maxActiveGoals
            Integer.MAX_VALUE, // unlimited
            Integer.MAX_VALUE, // unlimited
            Integer.MAX_VALUE, // unlimited
            20    // aiGenerationsPerMonth
    );

    /** Resolve limits for a tier code. */
    public static FeatureLimits forTier(String tierCode) {
        Objects.requireNonNull(tierCode);
        return switch (tierCode.toUpperCase()) {
            case "FREE" -> FREE;
            case "BASIC" -> BASIC;
            case "PREMIUM" -> PREMIUM;
            default -> FREE; // defensive fallback
        };
    }

    public int getMaxTrainingPlans() { return maxTrainingPlans; }
    public int getMaxWorkoutTemplates() { return maxWorkoutTemplates; }
    public int getMaxExercises() { return maxExercises; }
    public int getMaxActiveGoals() { return maxActiveGoals; }
    public int getMaxWorkoutLogs() { return maxWorkoutLogs; }
    public int getMaxAssessmentResults() { return maxAssessmentResults; }
    public int getMaxPlatformForks() { return maxPlatformForks; }
    public int getAiGenerationsPerMonth() { return aiGenerationsPerMonth; }

    public boolean isUnlimited(int value) {
        return value == Integer.MAX_VALUE;
    }
}
