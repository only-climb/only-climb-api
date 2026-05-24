package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public interface DeleteGoalUseCase {
    void delete(UUID goalId, UUID callerId);
}
