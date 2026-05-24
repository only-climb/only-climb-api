package app.onlyclimb.api.infrastructure.adapter.out.persistence.workout;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "workout_template_exercises", uniqueConstraints = @UniqueConstraint(
        name = "workout_template_exercises_template_id_position_key",
        columnNames = {"template_id", "position"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkoutTemplateExerciseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "position", nullable = false)
    private int position;

    /** JSONB map of {@code ParameterType.name() -> string value}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> config = new LinkedHashMap<>();
}
