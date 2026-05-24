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
@Table(name = "training_plan_weeks", uniqueConstraints = @UniqueConstraint(
        name = "training_plan_weeks_plan_id_week_number_key",
        columnNames = {"plan_id", "week_number"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TrainingPlanWeekJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Column(name = "is_deload", nullable = false)
    private boolean deload;
}
