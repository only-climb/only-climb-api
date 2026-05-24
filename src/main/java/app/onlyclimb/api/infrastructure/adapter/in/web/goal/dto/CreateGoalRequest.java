package app.onlyclimb.api.infrastructure.adapter.in.web.goal.dto;

import app.onlyclimb.api.domain.model.GoalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateGoalRequest(
        @NotNull GoalType type,
        @Valid ClimbingGradeDto targetGrade,
        LocalDate targetDate,
        String notes) {}
