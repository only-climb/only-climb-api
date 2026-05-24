package app.onlyclimb.api.infrastructure.adapter.out.persistence.workout;

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
@Table(name = "workout_template_translations", uniqueConstraints = @UniqueConstraint(
        name = "workout_template_translations_template_id_locale_field_key",
        columnNames = {"template_id", "locale", "field"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class WorkoutTemplateTranslationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "field", nullable = false, length = 50)
    private String field;

    @Column(name = "value", nullable = false, columnDefinition = "text")
    private String value;
}
