package app.onlyclimb.api.domain.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A subscription tier from the product catalog (FREE, BASIC, PREMIUM).
 * Marketing copy is stored as translations keyed by locale and field.
 */
public class SubscriptionTier {

    private final UUID id;
    private final String code;
    private final int sortOrder;
    private final boolean active;
    private final Map<String, Map<String, String>> translations; // locale -> (field -> value)

    public SubscriptionTier(UUID id, String code, int sortOrder, boolean active,
                            Map<String, Map<String, String>> translations) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.code = Objects.requireNonNull(code, "code is required");
        if (code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        this.sortOrder = sortOrder;
        this.active = active;
        this.translations = translations == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(translations);
    }

    public boolean isFree() {
        return "FREE".equalsIgnoreCase(code);
    }

    public boolean isActive() {
        return active;
    }

    public String getTranslation(String locale, String field) {
        Map<String, String> localeMap = translations.get(locale);
        return localeMap != null ? localeMap.getOrDefault(field, "") : "";
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public int getSortOrder() { return sortOrder; }
    public Map<String, Map<String, String>> getTranslations() { return translations; }
}
