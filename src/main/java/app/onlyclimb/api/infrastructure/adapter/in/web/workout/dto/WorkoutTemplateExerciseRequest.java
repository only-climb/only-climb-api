package app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto;

import app.onlyclimb.api.domain.model.ParameterType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WorkoutTemplateExerciseRequest(
        @Min(1) int position,
        @NotNull UUID exerciseId,
        Map<ParameterType, String> config,
        @Valid List<NotesTranslationDto> notesTranslations) {
}
