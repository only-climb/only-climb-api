package app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto;

import app.onlyclimb.api.domain.model.ParameterType;
import app.onlyclimb.api.domain.model.WorkoutTemplateExercise;

import java.util.Map;
import java.util.UUID;

public record WorkoutTemplateExerciseResponse(
        int position,
        UUID exerciseId,
        Map<ParameterType, String> config,
        String notes,
        String notesLocale) {

    public static WorkoutTemplateExerciseResponse from(WorkoutTemplateExercise entry, String locale) {
        return new WorkoutTemplateExerciseResponse(
                entry.getPosition(),
                entry.getExerciseId(),
                entry.getConfig(),
                entry.resolveNotes(locale).orElse(null),
                locale);
    }
}
