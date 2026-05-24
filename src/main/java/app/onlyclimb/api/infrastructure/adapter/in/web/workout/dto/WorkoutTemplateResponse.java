package app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto;

import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.WorkoutTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutTemplateResponse(
        UUID id,
        ContentSource source,
        UUID ownerId,
        UUID forkedFromId,
        ContentVisibility visibility,
        DifficultyLevel difficultyLevel,
        Integer estimatedDurationMinutes,
        ClimbingDiscipline targetDiscipline,
        String name,
        String description,
        String coachNotes,
        String locale,
        List<WorkoutTemplateExerciseResponse> exercises,
        Instant createdAt,
        Instant updatedAt) {

    public static WorkoutTemplateResponse from(WorkoutTemplate template, String locale) {
        return new WorkoutTemplateResponse(
                template.getId(),
                template.getSource(),
                template.getOwnerId().orElse(null),
                template.getForkedFromId().orElse(null),
                template.getVisibility(),
                template.getDifficultyLevel(),
                template.getEstimatedDurationMinutes().orElse(null),
                template.getTargetDiscipline().orElse(null),
                template.resolveField(WorkoutTemplate.FIELD_NAME, locale).orElse(null),
                template.resolveField(WorkoutTemplate.FIELD_DESCRIPTION, locale).orElse(null),
                template.resolveField(WorkoutTemplate.FIELD_COACH_NOTES, locale).orElse(null),
                locale,
                template.getExercises().stream()
                        .map(e -> WorkoutTemplateExerciseResponse.from(e, locale))
                        .toList(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
