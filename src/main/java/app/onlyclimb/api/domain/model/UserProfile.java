package app.onlyclimb.api.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Extended user attributes (1:1 with {@link User}). Lives as its own aggregate
 * root so it can be loaded and updated independently of the auth-bound user row.
 * All physical fields are optional except {@code locale}, which defaults to
 * "es" (Only Climb's primary language) to drive the i18n fallback.
 */
public class UserProfile {

    public static final String DEFAULT_LOCALE = "es";

    private final UUID id;
    private final UUID userId;
    private String displayName;
    private Weight weight;
    private Height height;
    private ClimbingDiscipline primaryDiscipline;
    private String locale;
    private final Instant createdAt;
    private Instant updatedAt;

    public UserProfile(
            UUID id,
            UUID userId,
            String displayName,
            Weight weight,
            Height height,
            ClimbingDiscipline primaryDiscipline,
            String locale,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Profile id is required");
        this.userId = Objects.requireNonNull(userId, "User id is required");
        this.displayName = normalizeDisplayName(displayName);
        this.weight = weight;
        this.height = height;
        this.primaryDiscipline = primaryDiscipline;
        this.locale = normalizeLocale(locale);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static UserProfile empty(UUID userId) {
        Instant now = Instant.now();
        return new UserProfile(
                UUID.randomUUID(), userId,
                null, null, null, null,
                DEFAULT_LOCALE, now, now);
    }

    public void update(
            String displayName,
            Weight weight,
            Height height,
            ClimbingDiscipline primaryDiscipline,
            String locale
    ) {
        this.displayName = normalizeDisplayName(displayName);
        this.weight = weight;
        this.height = height;
        this.primaryDiscipline = primaryDiscipline;
        this.locale = normalizeLocale(locale);
        this.updatedAt = Instant.now();
    }

    private static String normalizeDisplayName(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("Display name exceeds 100 characters");
        }
        return trimmed;
    }

    private static String normalizeLocale(String value) {
        if (value == null || value.isBlank()) return DEFAULT_LOCALE;
        String trimmed = value.trim();
        if (trimmed.length() > 10) {
            throw new IllegalArgumentException("Locale exceeds 10 characters");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Optional<String> getDisplayName() { return Optional.ofNullable(displayName); }
    public Optional<Weight> getWeight() { return Optional.ofNullable(weight); }
    public Optional<Height> getHeight() { return Optional.ofNullable(height); }
    public Optional<ClimbingDiscipline> getPrimaryDiscipline() { return Optional.ofNullable(primaryDiscipline); }
    public String getLocale() { return locale; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
