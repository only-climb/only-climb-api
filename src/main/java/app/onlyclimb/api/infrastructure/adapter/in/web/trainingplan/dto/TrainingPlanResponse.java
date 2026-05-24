package app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto;

import app.onlyclimb.api.domain.model.EquipmentRequirement;
import app.onlyclimb.api.domain.model.TrainingPlan;
import app.onlyclimb.api.domain.model.TrainingPlanSession;
import app.onlyclimb.api.domain.model.TrainingPlanWeek;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TrainingPlanResponse(
        UUID id,
        String source,
        String generationType,
        UUID ownerId,
        UUID forkedFromId,
        String visibility,
        String difficultyLevel,
        String targetDiscipline,
        String primaryGoal,
        List<String> secondaryGoals,
        ClimbingGradeDto targetGradeMin,
        ClimbingGradeDto targetGradeMax,
        int durationWeeks,
        int sessionsPerWeek,
        Integer avgSessionDurationMinutes,
        String trainingVolume,
        boolean requiresHangboard,
        boolean requiresCampusBoard,
        boolean requiresGymAccess,
        boolean requiresOutdoorClimbing,
        boolean recoveryFocused,
        List<EquipmentResponse> equipment,
        String name,
        String shortDescription,
        String description,
        String methodology,
        String prerequisites,
        String expectedOutcomes,
        String authorNotes,
        String coachingTips,
        List<WeekResponse> weeks,
        Instant createdAt,
        Instant updatedAt) {

    public static TrainingPlanResponse from(TrainingPlan plan, String locale) {
        return new TrainingPlanResponse(
                plan.getId(),
                plan.getSource().name(),
                plan.getGenerationType().name(),
                plan.getOwnerId().orElse(null),
                plan.getForkedFromId().orElse(null),
                plan.getVisibility().name(),
                plan.getDifficultyLevel().name(),
                plan.getTargetDiscipline().name(),
                plan.getPrimaryGoal().name(),
                plan.getSecondaryGoals().stream().map(Enum::name).toList(),
                plan.getTargetGradeMin()
                        .map(g -> new ClimbingGradeDto(g.getScale().name(), g.getValue()))
                        .orElse(null),
                plan.getTargetGradeMax()
                        .map(g -> new ClimbingGradeDto(g.getScale().name(), g.getValue()))
                        .orElse(null),
                plan.getDurationWeeks(),
                plan.getSessionsPerWeek(),
                plan.getAvgSessionDurationMinutes().orElse(null),
                plan.getTrainingVolume().name(),
                plan.isRequiresHangboard(),
                plan.isRequiresCampusBoard(),
                plan.isRequiresGymAccess(),
                plan.isRequiresOutdoorClimbing(),
                plan.isRecoveryFocused(),
                plan.getEquipment().stream()
                        .map(EquipmentResponse::from)
                        .toList(),
                plan.resolveField(TrainingPlan.FIELD_NAME, locale).orElse(null),
                plan.resolveField(TrainingPlan.FIELD_SHORT_DESCRIPTION, locale).orElse(null),
                plan.resolveField(TrainingPlan.FIELD_DESCRIPTION, locale).orElse(null),
                plan.resolveField(TrainingPlan.FIELD_METHODOLOGY, locale).orElse(null),
                plan.resolveField(TrainingPlan.FIELD_PREREQUISITES, locale).orElse(null),
                plan.resolveField(TrainingPlan.FIELD_EXPECTED_OUTCOMES, locale).orElse(null),
                plan.resolveField(TrainingPlan.FIELD_AUTHOR_NOTES, locale).orElse(null),
                plan.resolveField(TrainingPlan.FIELD_COACHING_TIPS, locale).orElse(null),
                plan.getWeeks().stream().map(w -> WeekResponse.from(w, locale)).toList(),
                plan.getCreatedAt(),
                plan.getUpdatedAt());
    }

    public record EquipmentResponse(String code, boolean optional) {
        public static EquipmentResponse from(EquipmentRequirement e) {
            return new EquipmentResponse(e.code(), e.optional());
        }
    }

    public record WeekResponse(
            UUID id,
            int weekNumber,
            boolean deload,
            String name,
            String focus,
            String notes,
            List<SessionResponse> sessions) {

        public static WeekResponse from(TrainingPlanWeek w, String locale) {
            return new WeekResponse(
                    w.getId(),
                    w.getWeekNumber(),
                    w.isDeload(),
                    w.resolveField(TrainingPlanWeek.FIELD_NAME, locale).orElse(null),
                    w.resolveField(TrainingPlanWeek.FIELD_FOCUS, locale).orElse(null),
                    w.resolveField(TrainingPlanWeek.FIELD_NOTES, locale).orElse(null),
                    w.getSessions().stream().map(s -> SessionResponse.from(s, locale)).toList());
        }
    }

    public record SessionResponse(
            UUID id,
            int dayOfWeek,
            int position,
            UUID workoutTemplateId,
            boolean optional,
            String notes) {

        public static SessionResponse from(TrainingPlanSession s, String locale) {
            return new SessionResponse(
                    s.getId(),
                    s.getDayOfWeek(),
                    s.getPosition(),
                    s.getWorkoutTemplateId(),
                    s.isOptional(),
                    s.resolveNotes(locale).orElse(null));
        }
    }
}
