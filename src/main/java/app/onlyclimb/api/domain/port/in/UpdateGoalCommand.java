package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.ClimbingGrade;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateGoalCommand(
        UUID goalId,
        UUID callerId,
        ClimbingGrade targetGrade,
        LocalDate targetDate,
        String notes) {}
