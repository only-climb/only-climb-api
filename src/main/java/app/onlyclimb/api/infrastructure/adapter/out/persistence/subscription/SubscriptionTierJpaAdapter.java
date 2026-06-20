package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import app.onlyclimb.api.domain.model.SubscriptionTier;
import app.onlyclimb.api.domain.port.out.SubscriptionTierRepository;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SubscriptionTierJpaAdapter implements SubscriptionTierRepository {

    private final SpringDataSubscriptionTierRepository springRepo;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<SubscriptionTier> findAllActive() {
        List<SubscriptionTierJpaEntity> entities = springRepo.findAllActive();
        Map<Long, Map<String, Map<String, String>>> translations = loadAllTranslations();
        return entities.stream()
                .map(e -> toDomain(e, translations.getOrDefault(e.getId(), Map.of())))
                .toList();
    }

    @Override
    public Optional<SubscriptionTier> findByCode(String code) {
        return springRepo.findByCode(code)
                .map(e -> {
                    Map<Long, Map<String, Map<String, String>>> translations = loadAllTranslations();
                    return toDomain(e, translations.getOrDefault(e.getId(), Map.of()));
                });
    }

    @Override
    public Optional<SubscriptionTier> findById(UUID id) {
        return springRepo.findByUuid(id)
                .map(e -> {
                    Map<Long, Map<String, Map<String, String>>> translations = loadAllTranslations();
                    return toDomain(e, translations.getOrDefault(e.getId(), Map.of()));
                });
    }

    private SubscriptionTier toDomain(SubscriptionTierJpaEntity entity,
                                      Map<String, Map<String, String>> translations) {
        return new SubscriptionTier(
                entity.getUuid(),
                entity.getCode(),
                entity.getSortOrder(),
                entity.isActive(),
                translations);
    }

    /** Loads all tier translations grouped by tier_id -> locale -> (field -> value). */
    private Map<Long, Map<String, Map<String, String>>> loadAllTranslations() {
        Map<Long, Map<String, Map<String, String>>> result = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT tier_id, locale, field, value FROM subscription_tier_translations ORDER BY tier_id, locale, field",
                (rs) -> {
                    long tierId = rs.getLong("tier_id");
                    String locale = rs.getString("locale");
                    String field = rs.getString("field");
                    String value = rs.getString("value");
                    result.computeIfAbsent(tierId, k -> new HashMap<>())
                            .computeIfAbsent(locale, k -> new HashMap<>())
                            .put(field, value);
                });
        return result;
    }
}
