package app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NotesTranslationDto(
        @NotBlank @Size(max = 10) String locale,
        @NotBlank @Pattern(regexp = "notes", message = "field must be 'notes'")
        String field,
        @NotBlank String value) {
}
