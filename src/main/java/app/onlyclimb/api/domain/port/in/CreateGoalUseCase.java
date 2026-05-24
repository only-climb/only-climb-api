package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.Goal;

public interface CreateGoalUseCase {
    Goal create(CreateGoalCommand command);
}
