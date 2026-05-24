package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.Translation;

import java.util.List;
import java.util.UUID;

public record UpdateWorkoutTemplateCommand(
        UUID templateId,
        UUID callerId,
        ContentVisibility visibility,
        DifficultyLevel difficultyLevel,
        Integer estimatedDurationMinutes,
        ClimbingDiscipline targetDiscipline,
        List<WorkoutTemplateExerciseEntry> exercises,
        List<Translation> translations) {
}
