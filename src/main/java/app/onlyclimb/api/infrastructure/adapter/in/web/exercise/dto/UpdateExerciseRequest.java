package app.onlyclimb.api.infrastructure.adapter.in.web.exercise.dto;

import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.ParameterType;
import app.onlyclimb.api.domain.model.SafetyWarningLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record UpdateExerciseRequest(
        @NotNull @Size(max = 50) String categoryCode,
        @NotNull @Size(max = 50) String primaryMuscleGroupCode,
        @NotNull DifficultyLevel difficultyLevel,
        @NotNull SafetyWarningLevel safetyWarningLevel,
        boolean requiresEquipment,
        boolean isUnilateral,
        @Min(1) Integer estimatedDurationMinutes,
        Set<ParameterType> allowedParameters,
        @NotEmpty @Valid List<TranslationDto> translations,
        ContentVisibility visibility) {
}
