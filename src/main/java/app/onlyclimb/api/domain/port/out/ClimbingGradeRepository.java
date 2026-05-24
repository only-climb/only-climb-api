package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.ClimbingGrade;

public interface ClimbingGradeRepository {

    /** True if the {@code (scale, value)} pair exists in the catalog. */
    boolean exists(ClimbingGrade grade);
}
