package app.onlyclimb.api.infrastructure.adapter.out.persistence.workout;

import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workout_templates")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkoutTemplateJpaEntity {

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

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "forked_from_id")
    private Long forkedFromId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "visibility", nullable = false, columnDefinition = "content_visibility")
    private ContentVisibility visibility;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "difficulty_level", nullable = false, columnDefinition = "difficulty_level")
    private DifficultyLevel difficultyLevel;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "target_discipline", columnDefinition = "climbing_discipline")
    private ClimbingDiscipline targetDiscipline;
}
