package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.GoalType;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.model.TrainingVolume;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Command to create a brand new {@code USER_CREATED} {@link
 * app.onlyclimb.api.domain.model.TrainingPlan TrainingPlan}.
 *
 * <p>{@code ownerId} is the caller's user id. All weeks' session
 * {@code workoutTemplateId}s must reference templates visible to the caller.</p>
 */
public record CreateTrainingPlanCommand(
        UUID ownerId,
        ContentVisibility visibility,
        DifficultyLevel difficultyLevel,
        ClimbingDiscipline targetDiscipline,
        GoalType primaryGoal,
        Set<GoalType> secondaryGoals,
        ClimbingGrade targetGradeMin,
        ClimbingGrade targetGradeMax,
        int durationWeeks,
        int sessionsPerWeek,
        Integer avgSessionDurationMinutes,
        TrainingVolume trainingVolume,
        boolean requiresHangboard,
        boolean requiresCampusBoard,
        boolean requiresGymAccess,
        boolean requiresOutdoorClimbing,
        boolean recoveryFocused,
        List<EquipmentRequirementInput> equipment,
        List<TrainingPlanWeekInput> weeks,
        List<Translation> translations) {
}
