package app.onlyclimb.api.domain.model;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A single test within an {@link AssessmentDefinition} (e.g. "max hang 20mm").
 * Read-only domain model — definitions are platform-managed catalog content.
 */
public class AssessmentTest {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PROTOCOL = "protocol";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            FIELD_NAME, FIELD_DESCRIPTION, FIELD_PROTOCOL);

    private final UUID id;
    private final String code;
    private final int position;
    private final String unit;
    private final AssessmentValueType valueType;
    /** key = locale + ":" + field */
    private final Map<String, Translation> translations;

    public AssessmentTest(
            UUID id,
            String code,
            int position,
            String unit,
            AssessmentValueType valueType,
            Iterable<Translation> translations) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.code = requireText(code, "code", 80);
        if (position < 0) {
            throw new IllegalArgumentException("position must be >= 0");
        }
        this.position = position;
        this.unit = requireText(unit, "unit", 20);
        this.valueType = Objects.requireNonNull(valueType, "valueType is required");
        this.translations = new LinkedHashMap<>();
        if (translations != null) {
            for (Translation t : translations) {
                if (!ALLOWED_FIELDS.contains(t.field())) {
                    throw new IllegalArgumentException(
                            "Unsupported translation field for AssessmentTest: " + t.field());
                }
                this.translations.put(t.locale() + ":" + t.field(), t);
            }
        }
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public int getPosition() { return position; }
    public String getUnit() { return unit; }
    public AssessmentValueType getValueType() { return valueType; }

    public java.util.List<Translation> getTranslations() {
        return java.util.List.copyOf(translations.values());
    }

    public Optional<String> resolveField(String field, String locale) {
        if (locale != null) {
            Translation exact = translations.get(locale.toLowerCase(Locale.ROOT) + ":" + field);
            if (exact != null) return Optional.of(exact.value());
        }
        Translation fallback = translations.get("es:" + field);
        if (fallback != null) return Optional.of(fallback.value());
        return translations.values().stream()
                .filter(t -> t.field().equals(field))
                .map(Translation::value)
                .findFirst();
    }

    private static String requireText(String value, String name, int maxLen) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLen) {
            throw new IllegalArgumentException(name + " exceeds " + maxLen + " characters");
        }
        return trimmed;
    }
}
