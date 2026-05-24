package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public interface DeleteTrainingPlanUseCase {
    void delete(UUID planId, UUID callerId);
}
