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
 * Command to update an existing {@code USER_CREATED} {@link
 * app.onlyclimb.api.domain.model.TrainingPlan TrainingPlan}. PLATFORM plans
 * are immutable and the application service will reject the call.
 *
 * <p>This is a full replacement: the supplied weeks, equipment, secondary
 * goals and translations entirely replace the existing collections.</p>
 */
public record UpdateTrainingPlanCommand(
        UUID planId,
        UUID callerId,
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
