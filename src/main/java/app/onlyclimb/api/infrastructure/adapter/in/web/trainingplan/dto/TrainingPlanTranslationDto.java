package app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TrainingPlanTranslationDto(
        @NotBlank @Size(max = 10) String locale,
        @NotBlank @Pattern(
                regexp = "name|short_description|description|methodology|prerequisites|"
                        + "expected_outcomes|author_notes|coaching_tips")
        String field,
        @NotBlank @Size(max = 5000) String value) {
}
