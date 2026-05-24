package app.onlyclimb.api.domain.port.in;

/**
 * Equipment requirement entry inside a training-plan command payload.
 *
 * <p>{@code code} must exist in the {@code equipment} catalog (validated by
 * the application service). {@code optional} differentiates {@code MUST have}
 * from {@code nice to have}.</p>
 */
public record EquipmentRequirementInput(String code, boolean optional) {
}
