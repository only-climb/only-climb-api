package app.onlyclimb.api.infrastructure.adapter.out.persistence.exercise;

import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.SafetyWarningLevel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ExerciseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "source", nullable = false, columnDefinition = "content_source")
    private ContentSource source;

    /** FK to users.id; mapped from owner UUID via repository lookup. */
    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "visibility", nullable = false, columnDefinition = "content_visibility")
    private ContentVisibility visibility;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "difficulty_level", nullable = false, columnDefinition = "difficulty_level")
    private DifficultyLevel difficultyLevel;

    @Column(name = "primary_muscle_group_id", nullable = false)
    private Long primaryMuscleGroupId;

    @Column(name = "requires_equipment", nullable = false)
    private boolean requiresEquipment;

    @Column(name = "is_unilateral", nullable = false)
    private boolean unilateral;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "safety_warning_level", nullable = false, columnDefinition = "safety_warning_level")
    private SafetyWarningLevel safetyWarningLevel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "exercise_allowed_parameters",
            joinColumns = @JoinColumn(name = "exercise_id"))
    private Set<AllowedParameterJpaEntity> allowedParameters = new HashSet<>();
}
