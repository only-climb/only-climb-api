package app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentRequirementRequest(
        @NotBlank @Size(max = 50) String code,
        boolean optional) {
}
