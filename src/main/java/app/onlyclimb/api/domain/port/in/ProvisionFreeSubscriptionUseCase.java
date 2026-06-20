package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public interface ProvisionFreeSubscriptionUseCase {
    /** Provisions a FREE/LIFETIME subscription for a newly registered user. */
    void provision(UUID userId);
}
