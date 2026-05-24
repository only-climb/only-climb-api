package app.onlyclimb.api.domain.model;

import java.util.Objects;

/**
 * Equipment item a {@link TrainingPlan} relies on.
 *
 * <p>{@code code} is the stable catalog code (e.g. {@code HANGBOARD},
 * {@code PULLUP_BAR}); the application service is responsible for verifying
 * the code exists in the {@code equipment} catalog.</p>
 *
 * <p>{@code optional} differentiates {@code MUST have} from {@code nice to
 * have}: a plan that <em>requires</em> a hangboard cannot be followed without
 * one; a plan that <em>optionally</em> uses a foam roller can be followed
 * without it (sessions referencing it become skippable).</p>
 */
public record EquipmentRequirement(String code, boolean optional) {

    public EquipmentRequirement {
        Objects.requireNonNull(code, "code is required");
        if (code.isBlank()) {
            throw new IllegalArgumentException("equipment code is required");
        }
        if (code.length() > 50) {
            throw new IllegalArgumentException("equipment code exceeds 50 characters");
        }
        code = code.trim();
    }
}
