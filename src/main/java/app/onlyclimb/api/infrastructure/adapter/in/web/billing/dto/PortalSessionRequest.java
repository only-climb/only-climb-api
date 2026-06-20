package app.onlyclimb.api.infrastructure.adapter.in.web.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record PortalSessionRequest(
        @NotBlank String returnUrl
) {}
