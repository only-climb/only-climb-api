package app.onlyclimb.api.infrastructure.adapter.in.web.exercise.dto;

import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.Exercise;
import app.onlyclimb.api.domain.model.ParameterType;
import app.onlyclimb.api.domain.model.SafetyWarningLevel;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        ContentSource source,
        UUID ownerId,
        String categoryCode,
        String primaryMuscleGroupCode,
        ContentVisibility visibility,
        DifficultyLevel difficultyLevel,
        SafetyWarningLevel safetyWarningLevel,
        boolean requiresEquipment,
        boolean unilateral,
        Integer estimatedDurationMinutes,
        Set<ParameterType> allowedParameters,
        String name,
        String shortDescription,
        String description,
        String locale,
        Instant createdAt,
        Instant updatedAt) {

    public static ExerciseResponse from(Exercise exercise, String locale) {
        String resolvedName = exercise.resolveField(Exercise.FIELD_NAME, locale).orElse(null);
        String resolvedShort = exercise.resolveField(Exercise.FIELD_SHORT_DESCRIPTION, locale).orElse(null);
        String resolvedDescription = exercise.resolveField(Exercise.FIELD_DESCRIPTION, locale).orElse(null);
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getSource(),
                exercise.getOwnerId().orElse(null),
                exercise.getCategoryCode(),
                exercise.getPrimaryMuscleGroupCode(),
                exercise.getVisibility(),
                exercise.getDifficultyLevel(),
                exercise.getSafetyWarningLevel(),
                exercise.isRequiresEquipment(),
                exercise.isUnilateral(),
                exercise.getEstimatedDurationMinutes().orElse(null),
                exercise.getAllowedParameters(),
                resolvedName,
                resolvedShort,
                resolvedDescription,
                locale,
                exercise.getCreatedAt(),
                exercise.getUpdatedAt());
    }
}
