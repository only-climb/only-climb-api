package app.onlyclimb.api.domain.model;

/**
 * Coarse-grained weekly volume bucket used to filter and surface
 * {@link TrainingPlan}s during search.
 */
public enum TrainingVolume {
    LOW,
    MODERATE,
    HIGH
}
