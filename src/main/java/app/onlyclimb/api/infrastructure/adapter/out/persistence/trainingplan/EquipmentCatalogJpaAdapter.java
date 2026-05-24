package app.onlyclimb.api.infrastructure.adapter.out.persistence.trainingplan;

import app.onlyclimb.api.domain.port.out.EquipmentCatalogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@Transactional(readOnly = true)
class EquipmentCatalogJpaAdapter implements EquipmentCatalogRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean exists(String code) {
        if (code == null || code.isBlank()) return false;
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(1) FROM equipment WHERE code = :code AND is_active = TRUE")
                .setParameter("code", code)
                .getSingleResult();
        return count.intValue() > 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> findMissing(Set<String> codes) {
        if (codes == null || codes.isEmpty()) return Set.of();
        List<String> found = entityManager.createNativeQuery(
                        "SELECT code FROM equipment WHERE code IN (:codes) AND is_active = TRUE")
                .setParameter("codes", codes)
                .getResultList();
        Set<String> missing = new LinkedHashSet<>(codes);
        missing.removeAll(found);
        return missing;
    }
}
