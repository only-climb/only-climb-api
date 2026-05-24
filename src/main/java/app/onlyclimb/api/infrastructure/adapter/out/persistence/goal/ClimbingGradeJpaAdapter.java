package app.onlyclimb.api.infrastructure.adapter.out.persistence.goal;

import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.port.out.ClimbingGradeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
class ClimbingGradeJpaAdapter implements ClimbingGradeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean exists(ClimbingGrade grade) {
        if (grade == null) return false;
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM climbing_grades " +
                                "WHERE scale = CAST(:scale AS grade_scale) AND value = :value")
                .setParameter("scale", grade.getScale().name())
                .setParameter("value", grade.getValue())
                .getSingleResult();
        return count.longValue() > 0;
    }
}
