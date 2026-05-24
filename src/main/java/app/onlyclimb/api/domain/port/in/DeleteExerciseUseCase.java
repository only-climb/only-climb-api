package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public interface DeleteExerciseUseCase {
    void delete(UUID exerciseId, UUID callerId);
}
