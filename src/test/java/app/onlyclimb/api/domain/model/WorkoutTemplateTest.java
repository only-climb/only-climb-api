package app.onlyclimb.api.domain.model;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.PlatformContentImmutableException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkoutTemplateTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final List<Translation> NAME_EN = List.of(
            new Translation("en", WorkoutTemplate.FIELD_NAME, "Fingerboard short session"));

    private static WorkoutTemplateExercise entry(int position) {
        return new WorkoutTemplateExercise(
                position, UUID.randomUUID(),
                Map.of(ParameterType.REPS, "5", ParameterType.SETS, "3"),
                List.of(new Translation("en", WorkoutTemplateExercise.FIELD_NOTES, "Warm up first")));
    }

    @Test
    void createUserTemplate_setsDefaults_andRenumbersPositions() {
        WorkoutTemplate t = WorkoutTemplate.createUserTemplate(
                OWNER, null, DifficultyLevel.INTERMEDIATE, 30, ClimbingDiscipline.BOULDER,
                List.of(entry(5), entry(2)), NAME_EN);

        assertThat(t.getSource()).isEqualTo(ContentSource.USER_CREATED);
        assertThat(t.getVisibility()).isEqualTo(ContentVisibility.PRIVATE);
        assertThat(t.getOwnerId()).contains(OWNER);
        assertThat(t.getForkedFromId()).isEmpty();
        assertThat(t.getExercises()).extracting(WorkoutTemplateExercise::getPosition)
                .containsExactly(1, 2);
        assertThat(t.isActive()).isTrue();
    }

    @Test
    void rejectsEmptyExerciseList() {
        assertThatThrownBy(() -> WorkoutTemplate.createUserTemplate(
                OWNER, null, DifficultyLevel.BEGINNER, null, null, List.of(), NAME_EN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one exercise");
    }

    @Test
    void rejectsDuplicatePositions() {
        assertThatThrownBy(() -> WorkoutTemplate.createUserTemplate(
                OWNER, null, DifficultyLevel.BEGINNER, null, null,
                List.of(entry(1), entry(1)), NAME_EN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate position");
    }

    @Test
    void rejectsMissingNameTranslation() {
        assertThatThrownBy(() -> WorkoutTemplate.createUserTemplate(
                OWNER, null, DifficultyLevel.BEGINNER, null, null,
                List.of(entry(1)), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name translation");
    }

    @Test
    void platformTemplate_mustBePublic_andOwnerless_andNotAFork() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new WorkoutTemplate(
                UUID.randomUUID(), ContentSource.PLATFORM, OWNER, null,
                ContentVisibility.PUBLIC, DifficultyLevel.BEGINNER, null, null,
                List.of(entry(1)), NAME_EN, now, now, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot have an owner");

        assertThatThrownBy(() -> new WorkoutTemplate(
                UUID.randomUUID(), ContentSource.PLATFORM, null, UUID.randomUUID(),
                ContentVisibility.PUBLIC, DifficultyLevel.BEGINNER, null, null,
                List.of(entry(1)), NAME_EN, now, now, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be forks");
    }

    @Test
    void platformTemplate_rejectsMutation() {
        Instant now = Instant.now();
        WorkoutTemplate platform = new WorkoutTemplate(
                UUID.randomUUID(), ContentSource.PLATFORM, null, null,
                ContentVisibility.PUBLIC, DifficultyLevel.BEGINNER, null, null,
                List.of(entry(1)), NAME_EN, now, now, null);
        assertThatThrownBy(() -> platform.changeVisibility(ContentVisibility.PRIVATE))
                .isInstanceOf(PlatformContentImmutableException.class);
        assertThatThrownBy(platform::softDelete)
                .isInstanceOf(PlatformContentImmutableException.class);
    }

    @Test
    void assertEditableBy_rejectsNonOwner() {
        WorkoutTemplate t = WorkoutTemplate.createUserTemplate(
                OWNER, null, DifficultyLevel.BEGINNER, null, null,
                List.of(entry(1)), NAME_EN);
        assertThatThrownBy(() -> t.assertEditableBy(UUID.randomUUID()))
                .isInstanceOf(ContentOwnershipException.class);
    }

    @Test
    void fork_isIndependentCopy_withForkedFromId() {
        WorkoutTemplate source = WorkoutTemplate.createUserTemplate(
                OWNER, ContentVisibility.PUBLIC, DifficultyLevel.ADVANCED, 45,
                ClimbingDiscipline.SPORT, List.of(entry(1), entry(2)), NAME_EN);

        UUID newOwner = UUID.randomUUID();
        WorkoutTemplate copy = source.fork(newOwner);

        assertThat(copy.getId()).isNotEqualTo(source.getId());
        assertThat(copy.getOwnerId()).contains(newOwner);
        assertThat(copy.getForkedFromId()).contains(source.getId());
        assertThat(copy.getVisibility()).isEqualTo(ContentVisibility.PRIVATE);
        assertThat(copy.getExercises()).hasSize(2);
        // Mutating the copy's entries does not affect the source's entries.
        assertThat(copy.getExercises().get(0))
                .isNotSameAs(source.getExercises().get(0));
        assertThat(copy.getExercises().get(0).getExerciseId())
                .isEqualTo(source.getExercises().get(0).getExerciseId());
    }

    @Test
    void resolveField_fallsBackToEnglish() {
        WorkoutTemplate t = WorkoutTemplate.createUserTemplate(
                OWNER, null, DifficultyLevel.BEGINNER, null, null,
                List.of(entry(1)),
                List.of(
                        new Translation("en", WorkoutTemplate.FIELD_NAME, "Hangboard"),
                        new Translation("es", WorkoutTemplate.FIELD_NAME, "Tabla")));
        assertThat(t.resolveField(WorkoutTemplate.FIELD_NAME, "es")).contains("Tabla");
        assertThat(t.resolveField(WorkoutTemplate.FIELD_NAME, "fr")).contains("Hangboard");
    }

    @Test
    void replaceExercises_validatesAndRenumbers() {
        WorkoutTemplate t = WorkoutTemplate.createUserTemplate(
                OWNER, null, DifficultyLevel.BEGINNER, null, null,
                List.of(entry(1)), NAME_EN);
        t.replaceExercises(List.of(entry(7), entry(3), entry(5)));
        assertThat(t.getExercises()).extracting(WorkoutTemplateExercise::getPosition)
                .containsExactly(1, 2, 3);
    }

    @Test
    void entry_rejectsNonNotesField() {
        assertThatThrownBy(() -> new WorkoutTemplateExercise(
                1, UUID.randomUUID(), Map.of(),
                List.of(new Translation("en", "name", "Wrong"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("notes");
    }
}
