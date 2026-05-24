package app.onlyclimb.api.infrastructure.adapter.in.web.goal.dto;

import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.model.GradeScale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClimbingGradeDto(
        @NotNull GradeScale scale,
        @NotBlank String value) {

    public ClimbingGrade toDomain() {
        return new ClimbingGrade(scale, value);
    }

    public static ClimbingGradeDto from(ClimbingGrade grade) {
        return new ClimbingGradeDto(grade.getScale(), grade.getValue());
    }
}
