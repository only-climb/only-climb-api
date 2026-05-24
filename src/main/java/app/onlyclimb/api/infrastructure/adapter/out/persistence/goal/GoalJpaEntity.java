package app.onlyclimb.api.infrastructure.adapter.out.persistence.goal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_goals")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class GoalJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "goal_type_id", nullable = false)
    private Long goalTypeId;

    @Column(name = "target_grade_id")
    private Long targetGradeId;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "achieved_at")
    private Instant achievedAt;

    @Column(name = "notes")
    private String notes;
}
