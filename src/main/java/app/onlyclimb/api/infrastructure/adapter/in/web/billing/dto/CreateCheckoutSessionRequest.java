package app.onlyclimb.api.infrastructure.adapter.in.web.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCheckoutSessionRequest(
        @NotNull UUID planId,
        @NotBlank String successUrl,
        @NotBlank String cancelUrl
) {}
