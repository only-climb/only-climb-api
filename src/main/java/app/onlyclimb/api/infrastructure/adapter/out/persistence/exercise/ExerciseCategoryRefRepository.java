package app.onlyclimb.api.infrastructure.adapter.out.persistence.exercise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ExerciseCategoryRefRepository extends JpaRepository<ExerciseCategoryRef, Long> {
    Optional<ExerciseCategoryRef> findByCode(String code);
    List<ExerciseCategoryRef> findByIdIn(List<Long> ids);
}
