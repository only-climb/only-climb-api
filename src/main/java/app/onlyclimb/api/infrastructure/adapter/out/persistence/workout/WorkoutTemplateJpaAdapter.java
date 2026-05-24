package app.onlyclimb.api.infrastructure.adapter.out.persistence.workout;

import app.onlyclimb.api.domain.model.ParameterType;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.model.WorkoutTemplate;
import app.onlyclimb.api.domain.model.WorkoutTemplateExercise;
import app.onlyclimb.api.domain.port.in.ListWorkoutTemplatesQuery;
import app.onlyclimb.api.domain.port.out.WorkoutTemplateRepository;
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
class WorkoutTemplateJpaAdapter implements WorkoutTemplateRepository {

    private final SpringDataWorkoutTemplateRepository templateRepo;
    private final SpringDataWorkoutTemplateTranslationRepository translationRepo;
    private final SpringDataWorkoutTemplateExerciseRepository exerciseRepo;
    private final SpringDataWorkoutTemplateExerciseTranslationRepository exerciseTranslationRepo;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public WorkoutTemplate save(WorkoutTemplate template) {
        WorkoutTemplateJpaEntity entity = templateRepo.findByUuid(template.getId())
                .orElseGet(WorkoutTemplateJpaEntity::new);
        if (entity.getId() == null) {
            entity.setUuid(template.getId());
            entity.setCreatedAt(template.getCreatedAt());
            entity.setSource(template.getSource());
            entity.setOwnerId(template.getOwnerId().map(this::resolveUserId).orElse(null));
            entity.setForkedFromId(template.getForkedFromId().map(this::resolveTemplateIdOrNull).orElse(null));
        }
        entity.setUpdatedAt(template.getUpdatedAt());
        entity.setDeletedAt(template.getDeletedAt().orElse(null));
        entity.setVisibility(template.getVisibility());
        entity.setDifficultyLevel(template.getDifficultyLevel());
        entity.setEstimatedDurationMinutes(template.getEstimatedDurationMinutes().orElse(null));
        entity.setTargetDiscipline(template.getTargetDiscipline().orElse(null));

        WorkoutTemplateJpaEntity persisted = templateRepo.save(entity);
        replaceTranslations(persisted.getId(), template.getTranslations());
        replaceExercises(persisted.getId(), template.getExercises());
        return toDomain(persisted);
    }

    @Override
    public Optional<WorkoutTemplate> findById(UUID id) {
        return templateRepo.findByUuid(id).map(this::toDomain);
    }

    @Override
    public Page<WorkoutTemplate> search(ListWorkoutTemplatesQuery query) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<WorkoutTemplateJpaEntity> cq = cb.createQuery(WorkoutTemplateJpaEntity.class);
        Root<WorkoutTemplateJpaEntity> root = cq.from(WorkoutTemplateJpaEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(root.get("deletedAt")));

        Long callerLongId = query.callerId() != null ? resolveUserIdOrNull(query.callerId()) : null;
        if (query.ownedOnly()) {
            if (callerLongId == null) {
                return new Page<>(List.of(), null);
            }
            predicates.add(cb.equal(root.get("ownerId"), callerLongId));
        } else {
            Predicate platform = cb.equal(root.get("source"), app.onlyclimb.api.domain.model.ContentSource.PLATFORM);
            Predicate publicUser = cb.and(
                    cb.equal(root.get("source"), app.onlyclimb.api.domain.model.ContentSource.USER_CREATED),
                    cb.equal(root.get("visibility"), app.onlyclimb.api.domain.model.ContentVisibility.PUBLIC));
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
        if (query.search() != null) {
            String pattern = "%" + query.search().toLowerCase() + "%";
            var sub = cq.subquery(Long.class);
            var tr = sub.from(WorkoutTemplateTranslationJpaEntity.class);
            sub.select(tr.get("templateId"))
                    .where(cb.and(
                            cb.equal(tr.get("field"), WorkoutTemplate.FIELD_NAME),
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

        List<WorkoutTemplateJpaEntity> rows = entityManager.createQuery(cq)
                .setMaxResults(query.limit() + 1)
                .getResultList();

        String nextCursor = null;
        if (rows.size() > query.limit()) {
            WorkoutTemplateJpaEntity last = rows.get(query.limit() - 1);
            nextCursor = encodeCursor(last.getCreatedAt(), last.getId());
            rows = rows.subList(0, query.limit());
        }
        List<WorkoutTemplate> domain = rows.stream().map(this::toDomain).toList();
        return new Page<>(domain, nextCursor);
    }

    // ---------------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------------

    private WorkoutTemplate toDomain(WorkoutTemplateJpaEntity entity) {
        List<WorkoutTemplateTranslationJpaEntity> trRows = translationRepo.findByTemplateId(entity.getId());
        Set<Translation> translations = trRows.stream()
                .map(r -> new Translation(r.getLocale(), r.getField(), r.getValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<WorkoutTemplateExerciseJpaEntity> entryRows =
                exerciseRepo.findByTemplateIdOrderByPosition(entity.getId());
        Map<Long, List<WorkoutTemplateExerciseTranslationJpaEntity>> entryTranslations =
                loadEntryTranslations(entryRows.stream().map(WorkoutTemplateExerciseJpaEntity::getId).toList());

        List<WorkoutTemplateExercise> entries = new ArrayList<>(entryRows.size());
        for (WorkoutTemplateExerciseJpaEntity row : entryRows) {
            entries.add(new WorkoutTemplateExercise(
                    row.getPosition(),
                    resolveExerciseUuid(row.getExerciseId()),
                    mapConfig(row.getConfig()),
                    entryTranslations.getOrDefault(row.getId(), List.of()).stream()
                            .map(t -> new Translation(t.getLocale(), t.getField(), t.getValue()))
                            .toList()));
        }

        return new WorkoutTemplate(
                entity.getUuid(),
                entity.getSource(),
                Optional.ofNullable(entity.getOwnerId()).map(this::resolveUserUuid).orElse(null),
                Optional.ofNullable(entity.getForkedFromId()).map(this::resolveTemplateUuid).orElse(null),
                entity.getVisibility(),
                entity.getDifficultyLevel(),
                entity.getEstimatedDurationMinutes(),
                entity.getTargetDiscipline(),
                entries,
                translations,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt());
    }

    private static Map<ParameterType, String> mapConfig(Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) return Map.of();
        Map<ParameterType, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            try {
                result.put(ParameterType.valueOf(e.getKey()), e.getValue());
            } catch (IllegalArgumentException ignored) {
                // Unknown legacy parameter — skip silently on read.
            }
        }
        return result;
    }

    private void replaceTranslations(Long templateId, Set<Translation> translations) {
        translationRepo.deleteByTemplateId(templateId);
        translationRepo.flush();
        for (Translation t : translations) {
            translationRepo.save(new WorkoutTemplateTranslationJpaEntity(
                    null, templateId, t.locale(), t.field(), t.value()));
        }
    }

    private void replaceExercises(Long templateId, List<WorkoutTemplateExercise> entries) {
        exerciseRepo.deleteByTemplateId(templateId);
        exerciseRepo.flush();
        for (WorkoutTemplateExercise entry : entries) {
            WorkoutTemplateExerciseJpaEntity row = new WorkoutTemplateExerciseJpaEntity();
            row.setTemplateId(templateId);
            row.setExerciseId(resolveExerciseId(entry.getExerciseId()));
            row.setPosition(entry.getPosition());
            Map<String, String> raw = new LinkedHashMap<>();
            entry.getConfig().forEach((k, v) -> raw.put(k.name(), v));
            row.setConfig(raw);
            WorkoutTemplateExerciseJpaEntity saved = exerciseRepo.save(row);
            for (Translation t : entry.getNotesTranslations()) {
                exerciseTranslationRepo.save(new WorkoutTemplateExerciseTranslationJpaEntity(
                        null, saved.getId(), t.locale(), t.field(), t.value()));
            }
        }
    }

    private Map<Long, List<WorkoutTemplateExerciseTranslationJpaEntity>> loadEntryTranslations(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        List<WorkoutTemplateExerciseTranslationJpaEntity> rows =
                exerciseTranslationRepo.findByTemplateExerciseIdIn(ids);
        Map<Long, List<WorkoutTemplateExerciseTranslationJpaEntity>> grouped = new HashMap<>();
        for (WorkoutTemplateExerciseTranslationJpaEntity r : rows) {
            grouped.computeIfAbsent(r.getTemplateExerciseId(), k -> new ArrayList<>()).add(r);
        }
        return grouped;
    }

    // ---------------------------------------------------------------------
    // UUID <-> id translation via native queries
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

    private Long resolveExerciseId(UUID exerciseUuid) {
        Object id = entityManager.createNativeQuery("SELECT id FROM exercises WHERE uuid = :uuid")
                .setParameter("uuid", exerciseUuid)
                .getSingleResult();
        return ((Number) id).longValue();
    }

    private UUID resolveExerciseUuid(Long exerciseId) {
        return (UUID) entityManager.createNativeQuery("SELECT uuid FROM exercises WHERE id = :id")
                .setParameter("id", exerciseId)
                .getSingleResult();
    }

    private Long resolveTemplateIdOrNull(UUID templateUuid) {
        try {
            Object id = entityManager.createNativeQuery(
                            "SELECT id FROM workout_templates WHERE uuid = :uuid")
                    .setParameter("uuid", templateUuid)
                    .getSingleResult();
            return ((Number) id).longValue();
        } catch (NoResultException e) {
            return null;
        }
    }

    private UUID resolveTemplateUuid(Long templateId) {
        return (UUID) entityManager.createNativeQuery(
                        "SELECT uuid FROM workout_templates WHERE id = :id")
                .setParameter("id", templateId)
                .getSingleResult();
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
