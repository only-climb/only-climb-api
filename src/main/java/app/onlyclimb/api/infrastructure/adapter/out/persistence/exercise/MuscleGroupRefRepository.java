package app.onlyclimb.api.infrastructure.adapter.out.persistence.exercise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface MuscleGroupRefRepository extends JpaRepository<MuscleGroupRef, Long> {
    Optional<MuscleGroupRef> findByCode(String code);
    List<MuscleGroupRef> findByIdIn(List<Long> ids);
}
