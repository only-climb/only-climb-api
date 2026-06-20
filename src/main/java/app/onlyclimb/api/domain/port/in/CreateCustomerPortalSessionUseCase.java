package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public interface CreateCustomerPortalSessionUseCase {
    record PortalSessionResponse(String portalUrl) {}

    PortalSessionResponse create(UUID userId, CreateCustomerPortalSessionCommand command);
}
