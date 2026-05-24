package app.onlyclimb.api.infrastructure.adapter.in.web.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TranslationDto(
        @NotBlank @Size(max = 10) String locale,
        @NotBlank @Pattern(regexp = "name|short_description|description",
                message = "field must be one of: name, short_description, description")
        String field,
        @NotBlank String value) {
}
