package app.onlyclimb.api.domain.model;

/**
 * Closed set of parameter types an {@link Exercise} can declare as configurable.
 * Mirrors the {@code parameter_types} catalog by {@code code}.
 */
public enum ParameterType {
    REPS,
    SETS,
    REST_SECONDS,
    DURATION_SECONDS,
    WEIGHT_KG,
    INTENSITY_PERCENT,
    EDGE_DEPTH_MM,
    GRIP_TYPE,
    RPE
}
