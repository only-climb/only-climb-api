package app.onlyclimb.api.infrastructure.adapter.out.persistence.exercise;

import app.onlyclimb.api.domain.model.Exercise;
import app.onlyclimb.api.domain.model.ParameterType;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.port.in.ListExercisesQuery;
import app.onlyclimb.api.domain.port.out.ExerciseRepository;
import jakarta.persistence.EntityManager;
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class ExerciseJpaAdapter implements ExerciseRepository {

    private final SpringDataExerciseRepository exerciseRepo;
    private final SpringDataExerciseTranslationRepository translationRepo;
    private final ExerciseCategoryRefRepository categoryRefRepo;
    private final MuscleGroupRefRepository muscleRefRepo;
    private final ParameterTypeRefRepository parameterRefRepo;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Exercise save(Exercise exercise) {
        ExerciseJpaEntity entity = exerciseRepo.findByUuid(exercise.getId())
                .orElseGet(ExerciseJpaEntity::new);
        Long categoryId = categoryRefRepo.findByCode(exercise.getCategoryCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown exercise category code: " + exercise.getCategoryCode()))
                .getId();
        Long muscleId = muscleRefRepo.findByCode(exercise.getPrimaryMuscleGroupCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown muscle group code: " + exercise.getPrimaryMuscleGroupCode()))
                .getId();
        if (entity.getId() == null) {
            entity.setUuid(exercise.getId());
            entity.setCreatedAt(exercise.getCreatedAt());
            entity.setSource(exercise.getSource());
            entity.setOwnerId(exercise.getOwnerId().map(this::resolveUserId).orElse(null));
        }
        entity.setUpdatedAt(exercise.getUpdatedAt());
        entity.setDeletedAt(exercise.getDeletedAt().orElse(null));
        entity.setCategoryId(categoryId);
        entity.setPrimaryMuscleGroupId(muscleId);
        entity.setVisibility(exercise.getVisibility());
        entity.setDifficultyLevel(exercise.getDifficultyLevel());
        entity.setRequiresEquipment(exercise.isRequiresEquipment());
        entity.setUnilateral(exercise.isUnilateral());
        entity.setEstimatedDurationMinutes(exercise.getEstimatedDurationMinutes().orElse(null));
        entity.setSafetyWarningLevel(exercise.getSafetyWarningLevel());

        Set<ParameterType> requested = exercise.getAllowedParameters();
        Set<AllowedParameterJpaEntity> allowed = new java.util.HashSet<>();
        if (!requested.isEmpty()) {
            List<String> codes = requested.stream().map(Enum::name).toList();
            Map<String, Long> idsByCode = parameterRefRepo.findByCodeIn(codes).stream()
                    .collect(Collectors.toMap(ParameterTypeRef::getCode, ParameterTypeRef::getId));
            for (String code : codes) {
                Long id = idsByCode.get(code);
                if (id == null) {
                    throw new IllegalStateException("Parameter type catalog missing code: " + code);
                }
                allowed.add(new AllowedParameterJpaEntity(id, false, null));
            }
        }
        entity.getAllowedParameters().clear();
        entity.getAllowedParameters().addAll(allowed);

        ExerciseJpaEntity persisted = exerciseRepo.save(entity);
        replaceTranslations(persisted.getId(), exercise.getTranslations());
        return toDomain(persisted, exercise.getTranslations());
    }

    @Override
    public Optional<Exercise> findById(UUID id) {
        return exerciseRepo.findByUuid(id).map(this::toDomain);
    }

    @Override
    public Page<Exercise> search(ListExercisesQuery query) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ExerciseJpaEntity> cq = cb.createQuery(ExerciseJpaEntity.class);
        Root<ExerciseJpaEntity> root = cq.from(ExerciseJpaEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(root.get("deletedAt")));

        // Visibility scope: PLATFORM ∪ USER_CREATED PUBLIC ∪ (owned private if callerId present)
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
        if (query.categoryCode() != null) {
            Optional<ExerciseCategoryRef> cat = categoryRefRepo.findByCode(query.categoryCode());
            if (cat.isEmpty()) {
                return new Page<>(List.of(), null);
            }
            predicates.add(cb.equal(root.get("categoryId"), cat.get().getId()));
        }
        if (query.search() != null) {
            // Match against any 'name' translation (case-insensitive).
            String pattern = "%" + query.search().toLowerCase() + "%";
            var sub = cq.subquery(Long.class);
            var tr = sub.from(ExerciseTranslationJpaEntity.class);
            sub.select(tr.get("exerciseId"))
                    .where(cb.and(
                            cb.equal(tr.get("field"), Exercise.FIELD_NAME),
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

        List<ExerciseJpaEntity> rows = entityManager.createQuery(cq)
                .setMaxResults(query.limit() + 1)
                .getResultList();

        String nextCursor = null;
        if (rows.size() > query.limit()) {
            ExerciseJpaEntity last = rows.get(query.limit() - 1);
            nextCursor = encodeCursor(last.getCreatedAt(), last.getId());
            rows = rows.subList(0, query.limit());
        }
        // Batch-load translations to avoid N+1.
        Map<Long, List<ExerciseTranslationJpaEntity>> translations = loadTranslationsFor(
                rows.stream().map(ExerciseJpaEntity::getId).toList());
        List<Exercise> domain = rows.stream()
                .map(e -> toDomain(e, mapTranslations(translations.getOrDefault(e.getId(), List.of()))))
                .toList();
        return new Page<>(domain, nextCursor);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Exercise toDomain(ExerciseJpaEntity entity) {
        List<ExerciseTranslationJpaEntity> rows = translationRepo.findByExerciseId(entity.getId());
        return toDomain(entity, mapTranslations(rows));
    }

    private Exercise toDomain(ExerciseJpaEntity entity, Set<Translation> translations) {
        return new Exercise(
                entity.getUuid(),
                entity.getSource(),
                Optional.ofNullable(entity.getOwnerId()).map(this::resolveUserUuid).orElse(null),
                resolveCategoryCode(entity.getCategoryId()),
                resolveMuscleCode(entity.getPrimaryMuscleGroupId()),
                entity.getVisibility(),
                entity.getDifficultyLevel(),
                entity.getSafetyWarningLevel(),
                entity.isRequiresEquipment(),
                entity.isUnilateral(),
                entity.getEstimatedDurationMinutes(),
                resolveParameterTypes(entity.getAllowedParameters()),
                translations,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt());
    }

    private void replaceTranslations(Long exerciseId, Set<Translation> translations) {
        translationRepo.deleteByExerciseId(exerciseId);
        translationRepo.flush();
        for (Translation t : translations) {
            translationRepo.save(new ExerciseTranslationJpaEntity(
                    null, exerciseId, t.locale(), t.field(), t.value()));
        }
    }

    private Map<Long, List<ExerciseTranslationJpaEntity>> loadTranslationsFor(List<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) return Map.of();
        List<ExerciseTranslationJpaEntity> all = entityManager.createQuery(
                        "SELECT t FROM ExerciseTranslationJpaEntity t WHERE t.exerciseId IN :ids",
                        ExerciseTranslationJpaEntity.class)
                .setParameter("ids", exerciseIds)
                .getResultList();
        Map<Long, List<ExerciseTranslationJpaEntity>> grouped = new HashMap<>();
        for (ExerciseTranslationJpaEntity t : all) {
            grouped.computeIfAbsent(t.getExerciseId(), k -> new ArrayList<>()).add(t);
        }
        return grouped;
    }

    private static Set<Translation> mapTranslations(List<ExerciseTranslationJpaEntity> rows) {
        return rows.stream()
                .map(r -> new Translation(r.getLocale(), r.getField(), r.getValue()))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Set<ParameterType> resolveParameterTypes(Set<AllowedParameterJpaEntity> rows) {
        if (rows == null || rows.isEmpty()) return EnumSet.noneOf(ParameterType.class);
        List<Long> ids = rows.stream().map(AllowedParameterJpaEntity::getParameterTypeId).toList();
        List<ParameterTypeRef> refs = parameterRefRepo.findByIdIn(ids);
        EnumSet<ParameterType> result = EnumSet.noneOf(ParameterType.class);
        for (ParameterTypeRef ref : refs) {
            try {
                result.add(ParameterType.valueOf(ref.getCode()));
            } catch (IllegalArgumentException ignored) {
                // Catalog row not modelled in the domain enum yet — skip.
            }
        }
        return result;
    }

    private String resolveCategoryCode(Long categoryId) {
        return categoryRefRepo.findById(categoryId)
                .map(ExerciseCategoryRef::getCode)
                .orElseThrow(() -> new IllegalStateException("Missing category row: " + categoryId));
    }

    private String resolveMuscleCode(Long muscleId) {
        return muscleRefRepo.findById(muscleId)
                .map(MuscleGroupRef::getCode)
                .orElseThrow(() -> new IllegalStateException("Missing muscle group row: " + muscleId));
    }

    private Long resolveUserId(UUID userUuid) {
        Object id = entityManager.createNativeQuery("SELECT id FROM users WHERE uuid = :uuid")
                .setParameter("uuid", userUuid)
                .getSingleResult();
        return ((Number) id).longValue();
    }

    private Long resolveUserIdOrNull(UUID userUuid) {
        try {
            return resolveUserId(userUuid);
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        }
    }

    private UUID resolveUserUuid(Long userId) {
        Object uuid = entityManager.createNativeQuery("SELECT uuid FROM users WHERE id = :id")
                .setParameter("id", userId)
                .getSingleResult();
        return (UUID) uuid;
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

    // Defensive accessor in case the empty-page branch returns
    @SuppressWarnings("unused")
    private static <T> List<T> empty() {
        return Collections.emptyList();
    }
}
