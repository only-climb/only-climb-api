package app.onlyclimb.api.infrastructure.adapter.in.web.goal.dto;

import jakarta.validation.Valid;

import java.time.LocalDate;

public record UpdateGoalRequest(
        @Valid ClimbingGradeDto targetGrade,
        LocalDate targetDate,
        String notes) {}
