package app.onlyclimb.api.infrastructure.adapter.out.persistence.trainingplan;

import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.PlanGenerationType;
import app.onlyclimb.api.domain.model.TrainingVolume;
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
@Table(name = "training_plans")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TrainingPlanJpaEntity {

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "generation_type", nullable = false, columnDefinition = "plan_generation_type")
    private PlanGenerationType generationType;

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "target_discipline", nullable = false, columnDefinition = "climbing_discipline")
    private ClimbingDiscipline targetDiscipline;

    @Column(name = "primary_goal_type_id", nullable = false)
    private Long primaryGoalTypeId;

    @Column(name = "target_grade_min_id")
    private Long targetGradeMinId;

    @Column(name = "target_grade_max_id")
    private Long targetGradeMaxId;

    @Column(name = "duration_weeks", nullable = false)
    private int durationWeeks;

    @Column(name = "sessions_per_week", nullable = false)
    private int sessionsPerWeek;

    @Column(name = "avg_session_duration_minutes")
    private Integer avgSessionDurationMinutes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "training_volume", nullable = false, columnDefinition = "training_volume")
    private TrainingVolume trainingVolume;

    @Column(name = "requires_hangboard", nullable = false)
    private boolean requiresHangboard;

    @Column(name = "requires_campus_board", nullable = false)
    private boolean requiresCampusBoard;

    @Column(name = "requires_gym_access", nullable = false)
    private boolean requiresGymAccess;

    @Column(name = "requires_outdoor_climbing", nullable = false)
    private boolean requiresOutdoorClimbing;

    @Column(name = "is_recovery_focused", nullable = false)
    private boolean recoveryFocused;
}
