package app.onlyclimb.api.infrastructure.adapter.in.web.goal.dto;

import app.onlyclimb.api.domain.model.Goal;
import app.onlyclimb.api.domain.model.GoalType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        UUID ownerId,
        GoalType type,
        ClimbingGradeDto targetGrade,
        LocalDate targetDate,
        boolean active,
        Instant achievedAt,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static GoalResponse from(Goal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getOwnerId(),
                goal.getType(),
                goal.getTargetGrade().map(ClimbingGradeDto::from).orElse(null),
                goal.getTargetDate().orElse(null),
                goal.isActive(),
                goal.getAchievedAt().orElse(null),
                goal.getNotes().orElse(null),
                goal.getCreatedAt(),
                goal.getUpdatedAt());
    }
}
