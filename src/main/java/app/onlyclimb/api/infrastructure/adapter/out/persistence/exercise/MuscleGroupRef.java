package app.onlyclimb.api.infrastructure.adapter.out.persistence.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "muscle_groups")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class MuscleGroupRef {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, unique = true)
    private String code;
}
