package app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto;

import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateWorkoutTemplateRequest(
        @NotNull DifficultyLevel difficultyLevel,
        @Min(1) Integer estimatedDurationMinutes,
        ClimbingDiscipline targetDiscipline,
        ContentVisibility visibility,
        @NotEmpty @Valid List<WorkoutTemplateExerciseRequest> exercises,
        @NotEmpty @Valid List<TranslationDto> translations) {
}
