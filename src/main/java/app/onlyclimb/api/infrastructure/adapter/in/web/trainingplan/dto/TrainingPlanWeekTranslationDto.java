package app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TrainingPlanWeekTranslationDto(
        @NotBlank @Size(max = 10) String locale,
        @NotBlank @Pattern(regexp = "name|focus|notes") String field,
        @NotBlank @Size(max = 5000) String value) {
}
