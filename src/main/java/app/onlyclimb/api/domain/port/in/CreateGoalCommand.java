package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.model.GoalType;

import java.time.LocalDate;
import java.util.UUID;

public record CreateGoalCommand(
        UUID ownerId,
        GoalType type,
        ClimbingGrade targetGrade,
        LocalDate targetDate,
        String notes) {}
