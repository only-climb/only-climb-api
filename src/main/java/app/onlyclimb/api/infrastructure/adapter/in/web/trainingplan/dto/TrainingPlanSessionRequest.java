package app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record TrainingPlanSessionRequest(
        @Min(1) @Max(7) int dayOfWeek,
        @Min(1) int position,
        @NotNull UUID workoutTemplateId,
        boolean optional,
        @Valid List<TrainingPlanSessionNotesDto> notesTranslations) {
}
