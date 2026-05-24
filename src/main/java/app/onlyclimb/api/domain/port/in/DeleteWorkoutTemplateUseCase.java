package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public interface DeleteWorkoutTemplateUseCase {
    void delete(UUID templateId, UUID callerId);
}
