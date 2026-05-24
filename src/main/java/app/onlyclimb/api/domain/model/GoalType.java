package app.onlyclimb.api.domain.model;

/**
 * Catalog of structured training objectives. Mirrors {@code goal_types.code}
 * rows seeded in V3.
 */
public enum GoalType {
    FINGER_STRENGTH(false),
    POWER_ENDURANCE(false),
    AEROBIC_BASE(false),
    GRADE_TARGET(true),
    ANTAGONIST(false),
    GENERAL_STRENGTH(false);

    private final boolean requiresTargetGrade;

    GoalType(boolean requiresTargetGrade) {
        this.requiresTargetGrade = requiresTargetGrade;
    }

    public boolean requiresTargetGrade() {
        return requiresTargetGrade;
    }
}
