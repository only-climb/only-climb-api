package app.onlyclimb.api.infrastructure.adapter.out.persistence.trainingplan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "training_plan_translations", uniqueConstraints = @UniqueConstraint(
        name = "training_plan_translations_plan_id_locale_field_key",
        columnNames = {"plan_id", "locale", "field"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class TrainingPlanTranslationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "field", nullable = false, length = 50)
    private String field;

    @Column(name = "value", nullable = false, columnDefinition = "text")
    private String value;
}
