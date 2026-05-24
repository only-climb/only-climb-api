package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.Goal;

import java.util.Optional;
import java.util.UUID;

public interface GetGoalUseCase {

    Goal getOwned(UUID goalId, UUID callerId);

    Optional<Goal> getCurrent(UUID callerId);
}
