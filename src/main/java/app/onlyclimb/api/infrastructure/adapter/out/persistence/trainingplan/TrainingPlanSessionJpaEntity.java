package app.onlyclimb.api.infrastructure.adapter.out.persistence.trainingplan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "training_plan_sessions", uniqueConstraints = @UniqueConstraint(
        name = "training_plan_sessions_week_id_day_of_week_position_key",
        columnNames = {"week_id", "day_of_week", "position"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TrainingPlanSessionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "week_id", nullable = false)
    private Long weekId;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "workout_template_id", nullable = false)
    private Long workoutTemplateId;

    @Column(name = "is_optional", nullable = false)
    private boolean optional;
}
