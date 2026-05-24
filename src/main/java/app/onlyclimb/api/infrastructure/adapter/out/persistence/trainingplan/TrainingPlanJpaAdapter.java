package app.onlyclimb.api.infrastructure.adapter.out.persistence.trainingplan;

import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.EquipmentRequirement;
import app.onlyclimb.api.domain.model.GoalType;
import app.onlyclimb.api.domain.model.GradeScale;
import app.onlyclimb.api.domain.model.TrainingPlan;
import app.onlyclimb.api.domain.model.TrainingPlanSession;
import app.onlyclimb.api.domain.model.TrainingPlanWeek;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.port.in.ListTrainingPlansQuery;
import app.onlyclimb.api.domain.port.out.TrainingPlanRepository;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class TrainingPlanJpaAdapter implements TrainingPlanRepository {

    private final SpringDataTrainingPlanRepository planRepo;
    private final SpringDataTrainingPlanTranslationRepository planTranslationRepo;
    private final SpringDataTrainingPlanWeekRepository weekRepo;
    private final SpringDataTrainingPlanWeekTranslationRepository weekTranslationRepo;
    private final SpringDataTrainingPlanSessionRepository sessionRepo;
    private final SpringDataTrainingPlanSessionTranslationRepository sessionTranslationRepo;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public TrainingPlan save(TrainingPlan plan) {
        TrainingPlanJpaEntity entity = planRepo.findByUuid(plan.getId())
                .orElseGet(TrainingPlanJpaEntity::new);
        if (entity.getId() == null) {
            entity.setUuid(plan.getId());
            entity.setCreatedAt(plan.getCreatedAt());
            entity.setSource(plan.getSource());
            entity.setGenerationType(plan.getGenerationType());
            entity.setOwnerId(plan.getOwnerId().map(this::resolveUserId).orElse(null));
            entity.setForkedFromId(plan.getForkedFromId().map(this::resolvePlanIdOrNull).orElse(null));
        }
        entity.setUpdatedAt(plan.getUpdatedAt());
        entity.setDeletedAt(plan.getDeletedAt().orElse(null));
        entity.setVisibility(plan.getVisibility());
        entity.setDifficultyLevel(plan.getDifficultyLevel());
        entity.setTargetDiscipline(plan.getTargetDiscipline());
        entity.setPrimaryGoalTypeId(resolveGoalTypeId(plan.getPrimaryGoal()));
        entity.setTargetGradeMinId(plan.getTargetGradeMin().map(this::resolveGradeId).orElse(null));
        entity.setTargetGradeMaxId(plan.getTargetGradeMax().map(this::resolveGradeId).orElse(null));
        entity.setDurationWeeks(plan.getDurationWeeks());
        entity.setSessionsPerWeek(plan.getSessionsPerWeek());
        entity.setAvgSessionDurationMinutes(plan.getAvgSessionDurationMinutes().orElse(null));
        entity.setTrainingVolume(plan.getTrainingVolume());
        entity.setRequiresHangboard(plan.isRequiresHangboard());
        entity.setRequiresCampusBoard(plan.isRequiresCampusBoard());
        entity.setRequiresGymAccess(plan.isRequiresGymAccess());
        entity.setRequiresOutdoorClimbing(plan.isRequiresOutdoorClimbing());
        entity.setRecoveryFocused(plan.isRecoveryFocused());

        TrainingPlanJpaEntity persisted = planRepo.save(entity);
        Long planId = persisted.getId();

        replacePlanTranslations(planId, plan.getTranslations());
        replaceSecondaryGoals(planId, plan.getSecondaryGoals());
        replaceEquipment(planId, plan.getEquipment());
        replaceWeeks(planId, plan.getWeeks());

        return toDomain(persisted);
    }

    @Override
    public Optional<TrainingPlan> findById(UUID id) {
        return planRepo.findByUuid(id).map(this::toDomain);
    }

    @Override
    public Page<TrainingPlan> search(ListTrainingPlansQuery query) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TrainingPlanJpaEntity> cq = cb.createQuery(TrainingPlanJpaEntity.class);
        Root<TrainingPlanJpaEntity> root = cq.from(TrainingPlanJpaEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(root.get("deletedAt")));

        Long callerLongId = query.callerId() != null ? resolveUserIdOrNull(query.callerId()) : null;
        if (query.ownedOnly()) {
            if (callerLongId == null) {
                return new Page<>(List.of(), null);
            }
            predicates.add(cb.equal(root.get("ownerId"), callerLongId));
        } else {
            Predicate platform = cb.equal(root.get("source"), ContentSource.PLATFORM);
            Predicate publicUser = cb.and(
                    cb.equal(root.get("source"), ContentSource.USER_CREATED),
                    cb.equal(root.get("visibility"), ContentVisibility.PUBLIC));
            if (callerLongId != null) {
                Predicate ownPrivate = cb.equal(root.get("ownerId"), callerLongId);
                predicates.add(cb.or(platform, publicUser, ownPrivate));
            } else {
                predicates.add(cb.or(platform, publicUser));
            }
        }
        if (query.difficulty() != null) {
            predicates.add(cb.equal(root.get("difficultyLevel"), query.difficulty()));
        }
        if (query.discipline() != null) {
            predicates.add(cb.equal(root.get("targetDiscipline"), query.discipline()));
        }
        if (query.volume() != null) {
            predicates.add(cb.equal(root.get("trainingVolume"), query.volume()));
        }
        if (query.primaryGoal() != null) {
            predicates.add(cb.equal(root.get("primaryGoalTypeId"),
                    resolveGoalTypeId(query.primaryGoal())));
        }
        if (query.search() != null) {
            String pattern = "%" + query.search().toLowerCase() + "%";
            var sub = cq.subquery(Long.class);
            var tr = sub.from(TrainingPlanTranslationJpaEntity.class);
            sub.select(tr.get("planId"))
                    .where(cb.and(
                            cb.equal(tr.get("field"), TrainingPlan.FIELD_NAME),
                            cb.like(cb.lower(tr.get("value")), pattern)));
            predicates.add(root.get("id").in(sub));
        }

        Cursor decoded = decodeCursor(query.cursor());
        if (decoded != null) {
            predicates.add(cb.or(
                    cb.lessThan(root.get("createdAt"), decoded.createdAt),
                    cb.and(
                            cb.equal(root.get("createdAt"), decoded.createdAt),
                            cb.lessThan(root.get("id"), decoded.id))));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));

        List<TrainingPlanJpaEntity> rows = entityManager.createQuery(cq)
                .setMaxResults(query.limit() + 1)
                .getResultList();

        String nextCursor = null;
        if (rows.size() > query.limit()) {
            TrainingPlanJpaEntity last = rows.get(query.limit() - 1);
            nextCursor = encodeCursor(last.getCreatedAt(), last.getId());
            rows = rows.subList(0, query.limit());
        }
        List<TrainingPlan> domain = rows.stream().map(this::toDomain).toList();
        return new Page<>(domain, nextCursor);
    }

    // ---------------------------------------------------------------------
    // Read mapping
    // ---------------------------------------------------------------------

    private TrainingPlan toDomain(TrainingPlanJpaEntity entity) {
        Long planId = entity.getId();

        Set<Translation> translations = planTranslationRepo.findByPlanId(planId).stream()
                .map(r -> new Translation(r.getLocale(), r.getField(), r.getValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<GoalType> secondary = loadSecondaryGoals(planId);
        Set<EquipmentRequirement> equipment = loadEquipment(planId);

        List<TrainingPlanWeekJpaEntity> weekRows = weekRepo.findByPlanIdOrderByWeekNumber(planId);
        List<Long> weekIds = weekRows.stream().map(TrainingPlanWeekJpaEntity::getId).toList();
        Map<Long, List<Translation>> weekTr = loadWeekTranslations(weekIds);

        Map<Long, List<TrainingPlanSessionJpaEntity>> sessionsByWeek = new HashMap<>();
        List<TrainingPlanSessionJpaEntity> sessionRows = weekIds.isEmpty()
                ? List.of()
                : sessionRepo.findByWeekIdInOrderByDayOfWeekAscPositionAsc(weekIds);
        for (TrainingPlanSessionJpaEntity s : sessionRows) {
            sessionsByWeek.computeIfAbsent(s.getWeekId(), k -> new ArrayList<>()).add(s);
        }
        Map<Long, List<Translation>> sessionTr = loadSessionTranslations(
                sessionRows.stream().map(TrainingPlanSessionJpaEntity::getId).toList());

        List<TrainingPlanWeek> weeks = new ArrayList<>(weekRows.size());
        for (TrainingPlanWeekJpaEntity wRow : weekRows) {
            List<TrainingPlanSessionJpaEntity> wSessions =
                    sessionsByWeek.getOrDefault(wRow.getId(), List.of());
            List<TrainingPlanSession> sessions = new ArrayList<>(wSessions.size());
            for (TrainingPlanSessionJpaEntity sRow : wSessions) {
                sessions.add(new TrainingPlanSession(
                        sRow.getUuid(),
                        sRow.getDayOfWeek(),
                        sRow.getPosition(),
                        resolveTemplateUuid(sRow.getWorkoutTemplateId()),
                        sRow.isOptional(),
                        sessionTr.getOrDefault(sRow.getId(), List.of())));
            }
            weeks.add(new TrainingPlanWeek(
                    wRow.getUuid(),
                    wRow.getWeekNumber(),
                    wRow.isDeload(),
                    sessions,
                    weekTr.getOrDefault(wRow.getId(), List.of())));
        }

        ClimbingDiscipline discipline = entity.getTargetDiscipline();
        ClimbingGrade gradeMin = entity.getTargetGradeMinId() == null ? null
                : resolveGrade(entity.getTargetGradeMinId());
        ClimbingGrade gradeMax = entity.getTargetGradeMaxId() == null ? null
                : resolveGrade(entity.getTargetGradeMaxId());

        return new TrainingPlan(
                entity.getUuid(),
                entity.getSource(),
                entity.getGenerationType(),
                Optional.ofNullable(entity.getOwnerId()).map(this::resolveUserUuid).orElse(null),
                Optional.ofNullable(entity.getForkedFromId()).map(this::resolvePlanUuid).orElse(null),
                entity.getVisibility(),
                entity.getDifficultyLevel(),
                discipline,
                resolveGoalType(entity.getPrimaryGoalTypeId()),
                secondary,
                gradeMin,
                gradeMax,
                entity.getDurationWeeks(),
                entity.getSessionsPerWeek(),
                entity.getAvgSessionDurationMinutes(),
                entity.getTrainingVolume(),
                entity.isRequiresHangboard(),
                entity.isRequiresCampusBoard(),
                entity.isRequiresGymAccess(),
                entity.isRequiresOutdoorClimbing(),
                entity.isRecoveryFocused(),
                equipment,
                weeks,
                translations,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt());
    }

    private Map<Long, List<Translation>> loadWeekTranslations(List<Long> weekIds) {
        if (weekIds.isEmpty()) return Map.of();
        Map<Long, List<Translation>> out = new HashMap<>();
        for (TrainingPlanWeekTranslationJpaEntity r : weekTranslationRepo.findByWeekIdIn(weekIds)) {
            out.computeIfAbsent(r.getWeekId(), k -> new ArrayList<>())
                    .add(new Translation(r.getLocale(), r.getField(), r.getValue()));
        }
        return out;
    }

    private Map<Long, List<Translation>> loadSessionTranslations(List<Long> sessionIds) {
        if (sessionIds.isEmpty()) return Map.of();
        Map<Long, List<Translation>> out = new HashMap<>();
        for (TrainingPlanSessionTranslationJpaEntity r : sessionTranslationRepo.findBySessionIdIn(sessionIds)) {
            out.computeIfAbsent(r.getSessionId(), k -> new ArrayList<>())
                    .add(new Translation(r.getLocale(), r.getField(), r.getValue()));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Set<GoalType> loadSecondaryGoals(Long planId) {
        List<String> codes = entityManager.createNativeQuery(
                        "SELECT gt.code FROM training_plan_secondary_goals s "
                                + "JOIN goal_types gt ON gt.id = s.goal_type_id "
                                + "WHERE s.plan_id = :planId")
                .setParameter("planId", planId)
                .getResultList();
        if (codes.isEmpty()) return Set.of();
        Set<GoalType> result = EnumSet.noneOf(GoalType.class);
        for (String c : codes) {
            try {
                result.add(GoalType.valueOf(c));
            } catch (IllegalArgumentException ignored) {
                // Unknown legacy code — skip on read.
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Set<EquipmentRequirement> loadEquipment(Long planId) {
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT e.code, te.is_optional FROM training_plan_equipment te "
                                + "JOIN equipment e ON e.id = te.equipment_id "
                                + "WHERE te.plan_id = :planId ORDER BY e.sort_order, e.code")
                .setParameter("planId", planId)
                .getResultList();
        Set<EquipmentRequirement> out = new LinkedHashSet<>();
        for (Object[] row : rows) {
            out.add(new EquipmentRequirement((String) row[0], (Boolean) row[1]));
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // Write mapping
    // ---------------------------------------------------------------------

    private void replacePlanTranslations(Long planId, Set<Translation> translations) {
        planTranslationRepo.deleteByPlanId(planId);
        planTranslationRepo.flush();
        for (Translation t : translations) {
            planTranslationRepo.save(new TrainingPlanTranslationJpaEntity(
                    null, planId, t.locale(), t.field(), t.value()));
        }
    }

    private void replaceSecondaryGoals(Long planId, Set<GoalType> goals) {
        entityManager.createNativeQuery(
                        "DELETE FROM training_plan_secondary_goals WHERE plan_id = :planId")
                .setParameter("planId", planId)
                .executeUpdate();
        for (GoalType g : goals) {
            entityManager.createNativeQuery(
                            "INSERT INTO training_plan_secondary_goals(plan_id, goal_type_id) "
                                    + "VALUES (:planId, :goalTypeId)")
                    .setParameter("planId", planId)
                    .setParameter("goalTypeId", resolveGoalTypeId(g))
                    .executeUpdate();
        }
    }

    private void replaceEquipment(Long planId, Set<EquipmentRequirement> equipment) {
        entityManager.createNativeQuery(
                        "DELETE FROM training_plan_equipment WHERE plan_id = :planId")
                .setParameter("planId", planId)
                .executeUpdate();
        for (EquipmentRequirement e : equipment) {
            entityManager.createNativeQuery(
                            "INSERT INTO training_plan_equipment(plan_id, equipment_id, is_optional) "
                                    + "VALUES (:planId, :equipmentId, :optional)")
                    .setParameter("planId", planId)
                    .setParameter("equipmentId", resolveEquipmentId(e.code()))
                    .setParameter("optional", e.optional())
                    .executeUpdate();
        }
    }

    private void replaceWeeks(Long planId, List<TrainingPlanWeek> weeks) {
        // Cascade-delete all sessions/translations via FK ON DELETE CASCADE on weeks.
        weekRepo.deleteByPlanId(planId);
        weekRepo.flush();
        for (TrainingPlanWeek w : weeks) {
            TrainingPlanWeekJpaEntity wEntity = new TrainingPlanWeekJpaEntity();
            wEntity.setUuid(w.getId());
            wEntity.setPlanId(planId);
            wEntity.setWeekNumber(w.getWeekNumber());
            wEntity.setDeload(w.isDeload());
            TrainingPlanWeekJpaEntity wSaved = weekRepo.save(wEntity);
            for (Translation t : w.getTranslations()) {
                weekTranslationRepo.save(new TrainingPlanWeekTranslationJpaEntity(
                        null, wSaved.getId(), t.locale(), t.field(), t.value()));
            }
            for (TrainingPlanSession s : w.getSessions()) {
                TrainingPlanSessionJpaEntity sEntity = new TrainingPlanSessionJpaEntity();
                sEntity.setUuid(s.getId());
                sEntity.setWeekId(wSaved.getId());
                sEntity.setDayOfWeek(s.getDayOfWeek());
                sEntity.setPosition(s.getPosition());
                sEntity.setWorkoutTemplateId(resolveTemplateId(s.getWorkoutTemplateId()));
                sEntity.setOptional(s.isOptional());
                TrainingPlanSessionJpaEntity sSaved = sessionRepo.save(sEntity);
                for (Translation t : s.getNotesTranslations()) {
                    sessionTranslationRepo.save(new TrainingPlanSessionTranslationJpaEntity(
                            null, sSaved.getId(), t.locale(), t.field(), t.value()));
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Catalog / cross-aggregate id resolution via native queries
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

    private Long resolveTemplateId(UUID templateUuid) {
        Object id = entityManager.createNativeQuery(
                        "SELECT id FROM workout_templates WHERE uuid = :uuid")
                .setParameter("uuid", templateUuid)
                .getSingleResult();
        return ((Number) id).longValue();
    }

    private UUID resolveTemplateUuid(Long templateId) {
        return (UUID) entityManager.createNativeQuery(
                        "SELECT uuid FROM workout_templates WHERE id = :id")
                .setParameter("id", templateId)
                .getSingleResult();
    }

    private Long resolvePlanIdOrNull(UUID planUuid) {
        try {
            Object id = entityManager.createNativeQuery(
                            "SELECT id FROM training_plans WHERE uuid = :uuid")
                    .setParameter("uuid", planUuid)
                    .getSingleResult();
            return ((Number) id).longValue();
        } catch (NoResultException e) {
            return null;
        }
    }

    private UUID resolvePlanUuid(Long planId) {
        return (UUID) entityManager.createNativeQuery(
                        "SELECT uuid FROM training_plans WHERE id = :id")
                .setParameter("id", planId)
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
        Object id = entityManager.createNativeQuery(
                        "SELECT id FROM climbing_grades WHERE scale = CAST(:scale AS grade_scale) "
                                + "AND value = :value")
                .setParameter("scale", grade.getScale().name())
                .setParameter("value", grade.getValue())
                .getSingleResult();
        return ((Number) id).longValue();
    }

    private ClimbingGrade resolveGrade(Long gradeId) {
        Object[] row = (Object[]) entityManager.createNativeQuery(
                        "SELECT scale, value FROM climbing_grades WHERE id = :id")
                .setParameter("id", gradeId)
                .getSingleResult();
        return new ClimbingGrade(GradeScale.valueOf((String) row[0]), (String) row[1]);
    }

    private Long resolveEquipmentId(String code) {
        Object id = entityManager.createNativeQuery(
                        "SELECT id FROM equipment WHERE code = :code")
                .setParameter("code", code)
                .getSingleResult();
        return ((Number) id).longValue();
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
