package app.onlyclimb.api.infrastructure.adapter.out.persistence.goal;

import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.model.Goal;
import app.onlyclimb.api.domain.model.GoalType;
import app.onlyclimb.api.domain.model.GradeScale;
import app.onlyclimb.api.domain.port.in.ListGoalsQuery;
import app.onlyclimb.api.domain.port.out.GoalRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class GoalJpaAdapter implements GoalRepository {

    private final SpringDataGoalRepository goalRepo;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Goal save(Goal goal) {
        GoalJpaEntity entity = goalRepo.findByUuid(goal.getId())
                .orElseGet(GoalJpaEntity::new);
        if (entity.getId() == null) {
            entity.setUuid(goal.getId());
            entity.setCreatedAt(goal.getCreatedAt());
            entity.setUserId(resolveUserId(goal.getOwnerId()));
            entity.setGoalTypeId(resolveGoalTypeId(goal.getType()));
        }
        entity.setUpdatedAt(goal.getUpdatedAt());
        entity.setTargetGradeId(goal.getTargetGrade()
                .map(this::resolveGradeId).orElse(null));
        entity.setTargetDate(goal.getTargetDate().orElse(null));
        entity.setActive(goal.isActive());
        entity.setAchievedAt(goal.getAchievedAt().orElse(null));
        entity.setNotes(goal.getNotes().orElse(null));
        return toDomain(goalRepo.save(entity));
    }

    @Override
    public Optional<Goal> findById(UUID id) {
        return goalRepo.findByUuid(id).map(this::toDomain);
    }

    @Override
    public Optional<Goal> findActiveByOwner(UUID ownerId) {
        Long userId = resolveUserIdOrNull(ownerId);
        if (userId == null) return Optional.empty();
        return goalRepo.findFirstByUserIdAndActiveTrue(userId).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        goalRepo.findByUuid(id).ifPresent(goalRepo::delete);
    }

    @Override
    public Page<Goal> search(ListGoalsQuery query) {
        Long ownerLongId = resolveUserIdOrNull(query.ownerId());
        if (ownerLongId == null) {
            return new Page<>(List.of(), null);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GoalJpaEntity> cq = cb.createQuery(GoalJpaEntity.class);
        Root<GoalJpaEntity> root = cq.from(GoalJpaEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("userId"), ownerLongId));
        if (Boolean.TRUE.equals(query.activeOnly())) {
            predicates.add(cb.isTrue(root.get("active")));
        }

        Cursor decoded = decodeCursor(query.cursor());
        if (decoded != null) {
            predicates.add(cb.or(
                    cb.lessThan(root.<Instant>get("createdAt"), decoded.createdAt),
                    cb.and(
                            cb.equal(root.get("createdAt"), decoded.createdAt),
                            cb.lessThan(root.get("id"), decoded.id))));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));

        List<GoalJpaEntity> rows = entityManager.createQuery(cq)
                .setMaxResults(query.limit() + 1)
                .getResultList();

        String nextCursor = null;
        if (rows.size() > query.limit()) {
            GoalJpaEntity last = rows.get(query.limit() - 1);
            nextCursor = encodeCursor(last.getCreatedAt(), last.getId());
            rows = rows.subList(0, query.limit());
        }
        List<Goal> domain = rows.stream().map(this::toDomain).toList();
        return new Page<>(domain, nextCursor);
    }

    // ---------------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------------

    private Goal toDomain(GoalJpaEntity e) {
        ClimbingGrade grade = e.getTargetGradeId() == null
                ? null
                : resolveGrade(e.getTargetGradeId());
        return new Goal(
                e.getUuid(),
                resolveUserUuid(e.getUserId()),
                resolveGoalType(e.getGoalTypeId()),
                grade,
                e.getTargetDate(),
                e.isActive(),
                e.getAchievedAt(),
                e.getNotes(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    // ---------------------------------------------------------------------
    // UUID/code <-> id translation
    // ---------------------------------------------------------------------

    private Long resolveUserId(UUID userUuid) {
        Object id = entityManager.createNativeQuery("SELECT id FROM users WHERE uuid = :uuid")
                .setParameter("uuid", userUuid)
                .getSingleResult();
        return ((Number) id).longValue();
    }

    private Long resolveUserIdOrNull(UUID userUuid) {
        try {
            return resolveUserId(userUuid);
        } catch (NoResultException e) {
            return null;
        }
    }

    private UUID resolveUserUuid(Long userId) {
        return (UUID) entityManager.createNativeQuery("SELECT uuid FROM users WHERE id = :id")
                .setParameter("id", userId)
                .getSingleResult();
    }

    private Long resolveGoalTypeId(GoalType type) {
        Object id = entityManager.createNativeQuery(
                        "SELECT id FROM goal_types WHERE code = :code")
                .setParameter("code", type.name())
                .getSingleResult();
        return ((Number) id).longValue();
    }

    private GoalType resolveGoalType(Long goalTypeId) {
        String code = (String) entityManager.createNativeQuery(
                        "SELECT code FROM goal_types WHERE id = :id")
                .setParameter("id", goalTypeId)
                .getSingleResult();
        return GoalType.valueOf(code);
    }

    private Long resolveGradeId(ClimbingGrade grade) {
        try {
            Object id = entityManager.createNativeQuery(
                            "SELECT id FROM climbing_grades " +
                                    "WHERE scale = CAST(:scale AS grade_scale) AND value = :value")
                    .setParameter("scale", grade.getScale().name())
                    .setParameter("value", grade.getValue())
                    .getSingleResult();
            return ((Number) id).longValue();
        } catch (NoResultException e) {
            return null;
        }
    }

    private ClimbingGrade resolveGrade(Long gradeId) {
        Object[] row = (Object[]) entityManager.createNativeQuery(
                        "SELECT scale, value FROM climbing_grades WHERE id = :id")
                .setParameter("id", gradeId)
                .getSingleResult();
        GradeScale scale = GradeScale.valueOf(row[0].toString());
        return new ClimbingGrade(scale, (String) row[1]);
    }

    // ---------------------------------------------------------------------
    // Cursor helpers
    // ---------------------------------------------------------------------

    private record Cursor(Instant createdAt, Long id) {}

    private static String encodeCursor(Instant createdAt, Long id) {
        String raw = createdAt.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decodeCursor(String encoded) {
        if (encoded == null) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            int sep = raw.indexOf(':');
            if (sep < 0) return null;
            Instant at = Instant.ofEpochMilli(Long.parseLong(raw.substring(0, sep)));
            Long id = Long.parseLong(raw.substring(sep + 1));
            return new Cursor(at, id);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
