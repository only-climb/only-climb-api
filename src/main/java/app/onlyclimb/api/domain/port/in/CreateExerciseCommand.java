package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.ParameterType;
import app.onlyclimb.api.domain.model.SafetyWarningLevel;
import app.onlyclimb.api.domain.model.Translation;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Payload for creating a new user-created exercise. */
public record CreateExerciseCommand(
        UUID ownerId,
        String categoryCode,
        String primaryMuscleGroupCode,
        DifficultyLevel difficultyLevel,
        SafetyWarningLevel safetyWarningLevel,
        boolean requiresEquipment,
        boolean isUnilateral,
        Integer estimatedDurationMinutes,
        Set<ParameterType> allowedParameters,
        List<Translation> translations,
        ContentVisibility visibility) {
}
