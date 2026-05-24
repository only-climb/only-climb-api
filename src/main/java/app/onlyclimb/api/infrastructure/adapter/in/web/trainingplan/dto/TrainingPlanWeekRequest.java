package app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.util.List;

public record TrainingPlanWeekRequest(
        @Min(1) int weekNumber,
        boolean deload,
        @Valid List<TrainingPlanSessionRequest> sessions,
        @Valid List<TrainingPlanWeekTranslationDto> translations) {
}
