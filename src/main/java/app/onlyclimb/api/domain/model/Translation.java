package app.onlyclimb.api.domain.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Localized text snippet attached to a translatable aggregate.
 * Fields are bounded to a small whitelist per aggregate to keep the
 * persistence schema stable.
 */
public record Translation(String locale, String field, String value) {

    public Translation {
        if (locale == null || locale.isBlank()) {
            throw new IllegalArgumentException("locale is required");
        }
        if (locale.length() > 10) {
            throw new IllegalArgumentException("locale exceeds 10 characters");
        }
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field is required");
        }
        if (field.length() > 50) {
            throw new IllegalArgumentException("field exceeds 50 characters");
        }
        Objects.requireNonNull(value, "value is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        locale = locale.toLowerCase(Locale.ROOT);
    }
}
