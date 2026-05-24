package app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TranslationDto(
        @NotBlank @Size(max = 10) String locale,
        @NotBlank @Pattern(regexp = "name|description|coach_notes",
                message = "field must be one of: name, description, coach_notes")
        String field,
        @NotBlank String value) {
}
