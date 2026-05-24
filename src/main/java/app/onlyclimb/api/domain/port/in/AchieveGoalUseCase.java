package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.Goal;

import java.util.UUID;

public interface AchieveGoalUseCase {
    Goal achieve(UUID goalId, UUID callerId);
}
