package app.onlyclimb.api.domain.model;

import java.util.Objects;

/**
 * Immutable value object identifying a grade as the pair {@code (scale, value)}.
 *
 * <p>The actual list of valid values lives in the {@code climbing_grades}
 * catalog table — this VO only enforces structural validity (non-blank value,
 * reasonable length). Existence in the catalog must be verified by the
 * application service.</p>
 */
public final class ClimbingGrade {

    private final GradeScale scale;
    private final String value;

    public ClimbingGrade(GradeScale scale, String value) {
        this.scale = Objects.requireNonNull(scale, "scale is required");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("grade value cannot be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 10) {
            throw new IllegalArgumentException("grade value too long: " + trimmed);
        }
        this.value = trimmed;
    }

    public GradeScale getScale() { return scale; }
    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClimbingGrade other)) return false;
        return scale == other.scale && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scale, value);
    }

    @Override
    public String toString() {
        return scale + ":" + value;
    }
}
