package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public record CreateCheckoutSessionCommand(
        UUID planId,
        String successUrl,
        String cancelUrl) {
}
