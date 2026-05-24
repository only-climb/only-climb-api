package app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto;

import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.GoalType;
import app.onlyclimb.api.domain.model.TrainingVolume;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public record CreateTrainingPlanRequest(
        @NotNull ContentVisibility visibility,
        @NotNull DifficultyLevel difficultyLevel,
        @NotNull ClimbingDiscipline targetDiscipline,
        @NotNull GoalType primaryGoal,
        Set<GoalType> secondaryGoals,
        @Valid ClimbingGradeDto targetGradeMin,
        @Valid ClimbingGradeDto targetGradeMax,
        @Min(1) @Max(104) int durationWeeks,
        @Min(1) @Max(14) int sessionsPerWeek,
        @Min(5) @Max(600) Integer avgSessionDurationMinutes,
        TrainingVolume trainingVolume,
        boolean requiresHangboard,
        boolean requiresCampusBoard,
        boolean requiresGymAccess,
        boolean requiresOutdoorClimbing,
        boolean recoveryFocused,
        @Valid List<EquipmentRequirementRequest> equipment,
        @Valid List<TrainingPlanWeekRequest> weeks,
        @NotEmpty @Valid List<TrainingPlanTranslationDto> translations) {
}
