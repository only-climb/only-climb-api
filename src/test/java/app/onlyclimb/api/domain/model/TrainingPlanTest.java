package app.onlyclimb.api.domain.model;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.PlatformContentImmutableException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingPlanTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final UUID TEMPLATE = UUID.randomUUID();
    private static final List<Translation> NAME_ES = List.of(
            new Translation("es", TrainingPlan.FIELD_NAME, "Plan"));

    private TrainingPlanWeek week(int number) {
        return new TrainingPlanWeek(
                UUID.randomUUID(), number, false,
                List.of(new TrainingPlanSession(
                        UUID.randomUUID(), 2, 1, TEMPLATE, false, List.of())),
                List.of());
    }

    private TrainingPlan userPlan() {
        return TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(week(1), week(2)), NAME_ES);
    }

    @Test
    void createUserPlanRequiresNameTranslation() {
        assertThatThrownBy(() -> TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name translation");
    }

    @Test
    void createUserPlanRejectsSecondaryEqualToPrimary() {
        assertThatThrownBy(() -> TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH,
                EnumSet.of(GoalType.FINGER_STRENGTH),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(week(1)), NAME_ES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Secondary goal");
    }

    @Test
    void createUserPlanRejectsGradeScaleMismatch() {
        ClimbingGrade boulderGrade = new ClimbingGrade(GradeScale.FONTAINEBLEAU, "7A");
        assertThatThrownBy(() -> TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.GRADE_TARGET, Set.of(),
                boulderGrade, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(week(1)), NAME_ES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match discipline");
    }

    @Test
    void createUserPlanRejectsDuplicateEquipmentCode() {
        assertThatThrownBy(() -> TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                List.of(new EquipmentRequirement("HANGBOARD", false),
                        new EquipmentRequirement("HANGBOARD", true)).stream()
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)),
                List.of(week(1)), NAME_ES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate equipment");
    }

    @Test
    void createUserPlanRejectsDuplicateWeekNumbers() {
        assertThatThrownBy(() -> TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(week(1), week(1)), NAME_ES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate weekNumber");
    }

    @Test
    void createUserPlanRejectsMoreWeeksThanDuration() {
        assertThatThrownBy(() -> TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 1, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(week(1), week(2)), NAME_ES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds plan duration");
    }

    @Test
    void platformPlanIsImmutable() {
        TrainingPlan platform = new TrainingPlan(
                UUID.randomUUID(), ContentSource.PLATFORM, PlanGenerationType.MANUAL,
                null, null, ContentVisibility.PUBLIC, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(week(1)), NAME_ES,
                java.time.Instant.now(), java.time.Instant.now(), null);
        assertThatThrownBy(() -> platform.changeVisibility(ContentVisibility.PRIVATE))
                .isInstanceOf(PlatformContentImmutableException.class);
        assertThatThrownBy(platform::softDelete)
                .isInstanceOf(PlatformContentImmutableException.class);
    }

    @Test
    void assertEditableByOtherFails() {
        TrainingPlan plan = userPlan();
        assertThatThrownBy(() -> plan.assertEditableBy(STRANGER))
                .isInstanceOf(ContentOwnershipException.class);
    }

    @Test
    void forkIsIndependentDeepCopy() {
        TrainingPlan source = userPlan();
        TrainingPlan fork = source.fork(STRANGER);

        assertThat(fork.getId()).isNotEqualTo(source.getId());
        assertThat(fork.getOwnerId()).contains(STRANGER);
        assertThat(fork.getSource()).isEqualTo(ContentSource.USER_CREATED);
        assertThat(fork.getGenerationType()).isEqualTo(PlanGenerationType.FORKED);
        assertThat(fork.getForkedFromId()).contains(source.getId());
        assertThat(fork.getVisibility()).isEqualTo(ContentVisibility.PRIVATE);
        assertThat(fork.getWeeks()).hasSameSizeAs(source.getWeeks());
        // Week and session ids must be fresh
        assertThat(fork.getWeeks().get(0).getId())
                .isNotEqualTo(source.getWeeks().get(0).getId());

        // Mutating the fork must not touch the source.
        fork.changeVisibility(ContentVisibility.PUBLIC);
        assertThat(source.getVisibility()).isEqualTo(ContentVisibility.PRIVATE);
    }

    @Test
    void softDeleteIsIdempotent() {
        TrainingPlan plan = userPlan();
        plan.softDelete();
        java.time.Instant first = plan.getDeletedAt().orElseThrow();
        plan.softDelete();
        assertThat(plan.getDeletedAt()).contains(first);
        assertThat(plan.isActive()).isFalse();
    }

    @Test
    void resolveFieldFallsBackToSpanish() {
        TrainingPlan plan = TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(week(1)),
                List.of(new Translation("es", TrainingPlan.FIELD_NAME, "Plan ES")));
        assertThat(plan.resolveField(TrainingPlan.FIELD_NAME, "en")).contains("Plan ES");
    }
}
