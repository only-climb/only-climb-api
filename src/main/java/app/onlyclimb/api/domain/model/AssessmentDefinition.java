package app.onlyclimb.api.domain.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Platform-managed catalog entry that describes a battery of assessment
 * {@link AssessmentTest tests} a user can perform. Definitions are seeded
 * by the platform and are read-only from the application perspective.
 */
public class AssessmentDefinition {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PROTOCOL = "protocol";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            FIELD_NAME, FIELD_DESCRIPTION, FIELD_PROTOCOL);

    private final UUID id;
    private final String code;
    private final ClimbingDiscipline targetDiscipline;
    private final boolean active;
    private final List<AssessmentTest> tests;
    /** key = locale + ":" + field */
    private final Map<String, Translation> translations;
    private final Instant createdAt;
    private final Instant updatedAt;

    public AssessmentDefinition(
            UUID id,
            String code,
            ClimbingDiscipline targetDiscipline,
            boolean active,
            List<AssessmentTest> tests,
            Iterable<Translation> translations,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (code.length() > 80) {
            throw new IllegalArgumentException("code exceeds 80 characters");
        }
        this.code = code.trim();
        this.targetDiscipline = targetDiscipline;
        this.active = active;
        this.tests = (tests == null) ? List.of() : List.copyOf(tests);
        this.translations = new LinkedHashMap<>();
        if (translations != null) {
            for (Translation t : translations) {
                if (!ALLOWED_FIELDS.contains(t.field())) {
                    throw new IllegalArgumentException(
                            "Unsupported translation field for AssessmentDefinition: " + t.field());
                }
                this.translations.put(t.locale() + ":" + t.field(), t);
            }
        }
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public Optional<ClimbingDiscipline> getTargetDiscipline() {
        return Optional.ofNullable(targetDiscipline);
    }
    public boolean isActive() { return active; }
    public List<AssessmentTest> getTests() { return tests; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public List<Translation> getTranslations() {
        return List.copyOf(translations.values());
    }

    public Optional<AssessmentTest> findTest(UUID testId) {
        if (testId == null) return Optional.empty();
        return tests.stream().filter(t -> t.getId().equals(testId)).findFirst();
    }

    public Set<UUID> testIds() {
        java.util.LinkedHashSet<UUID> ids = new java.util.LinkedHashSet<>();
        for (AssessmentTest t : tests) ids.add(t.getId());
        return java.util.Collections.unmodifiableSet(ids);
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
}
