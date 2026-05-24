package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.Goal;

public interface UpdateGoalUseCase {
    Goal update(UpdateGoalCommand command);
}
