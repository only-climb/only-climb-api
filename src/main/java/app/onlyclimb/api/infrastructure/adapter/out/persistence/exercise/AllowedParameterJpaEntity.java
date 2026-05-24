package app.onlyclimb.api.infrastructure.adapter.out.persistence.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(of = "parameterTypeId")
class AllowedParameterJpaEntity {

    @Column(name = "parameter_type_id", nullable = false)
    private Long parameterTypeId;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "default_value", length = 50)
    private String defaultValue;
}
