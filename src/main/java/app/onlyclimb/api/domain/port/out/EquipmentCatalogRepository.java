package app.onlyclimb.api.domain.port.out;

import java.util.Set;

/**
 * Lookup port for the {@code equipment} catalog. The implementation must
 * answer in terms of the stable {@code code} column — never an internal id.
 */
public interface EquipmentCatalogRepository {

    /** True if the equipment code exists and is active. */
    boolean exists(String code);

    /** Subset of {@code codes} that are unknown (or inactive). */
    Set<String> findMissing(Set<String> codes);
}
