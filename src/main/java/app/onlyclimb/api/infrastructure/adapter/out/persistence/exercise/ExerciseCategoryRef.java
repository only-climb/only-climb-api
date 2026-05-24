package app.onlyclimb.api.infrastructure.adapter.out.persistence.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Minimal JPA view of a catalog row (used for code ↔ id translation).
 * Catalog tables share the same shape ({@code id, uuid, code}); this entity
 * type is reused by mapping it to different physical tables in subclasses.
 */
@Entity
@Table(name = "exercise_categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ExerciseCategoryRef {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, unique = true)
    private String code;
}
