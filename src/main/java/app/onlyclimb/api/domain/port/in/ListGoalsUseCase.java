package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.Goal;
import app.onlyclimb.api.domain.port.out.GoalRepository.Page;

public interface ListGoalsUseCase {
    Page<Goal> list(ListGoalsQuery query);
}
