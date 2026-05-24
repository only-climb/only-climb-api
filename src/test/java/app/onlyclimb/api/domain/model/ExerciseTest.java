package app.onlyclimb.api.domain.model;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.PlatformContentImmutableException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExerciseTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final List<Translation> NAME_EN = List.of(
            new Translation("en", Exercise.FIELD_NAME, "Hangboard repeaters"));

    @Test
    void createUserExercise_setsDefaults_andRequiresOwner() {
        Exercise e = Exercise.createUserExercise(
                OWNER, "HANGBOARD", "FINGERS",
                DifficultyLevel.INTERMEDIATE, SafetyWarningLevel.MODERATE,
                true, false, 20,
                Set.of(ParameterType.REPS, ParameterType.REST_SECONDS),
                NAME_EN, null);

        assertThat(e.getSource()).isEqualTo(ContentSource.USER_CREATED);
        assertThat(e.getOwnerId()).contains(OWNER);
        assertThat(e.getVisibility()).isEqualTo(ContentVisibility.PRIVATE);
        assertThat(e.isActive()).isTrue();
        assertThat(e.getAllowedParameters())
                .containsExactlyInAnyOrder(ParameterType.REPS, ParameterType.REST_SECONDS);
    }

    @Test
    void userExercise_requiresOwner() {
        assertThatThrownBy(() -> Exercise.createUserExercise(
                null, "HANGBOARD", "FINGERS",
                DifficultyLevel.INTERMEDIATE, SafetyWarningLevel.NONE,
                false, false, null, Set.of(), NAME_EN, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void requiresAtLeastOneNameTranslation() {
        assertThatThrownBy(() -> Exercise.createUserExercise(
                OWNER, "HANGBOARD", "FINGERS",
                DifficultyLevel.BEGINNER, SafetyWarningLevel.NONE,
                false, false, null, Set.of(), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name translation");
    }

    @Test
    void platformExercise_rejectsMutation() {
        Instant now = Instant.now();
        Exercise platform = new Exercise(
                UUID.randomUUID(), ContentSource.PLATFORM, null,
                "HANGBOARD", "FINGERS",
                ContentVisibility.PUBLIC, DifficultyLevel.BEGINNER, SafetyWarningLevel.NONE,
                false, false, null, Set.of(),
                NAME_EN, now, now, null);

        assertThatThrownBy(() -> platform.changeVisibility(ContentVisibility.PRIVATE))
                .isInstanceOf(PlatformContentImmutableException.class);
        assertThatThrownBy(platform::softDelete)
                .isInstanceOf(PlatformContentImmutableException.class);
    }

    @Test
    void platformExercise_mustBePublic_andOwnerless() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new Exercise(
                UUID.randomUUID(), ContentSource.PLATFORM, OWNER,
                "HANGBOARD", "FINGERS",
                ContentVisibility.PUBLIC, DifficultyLevel.BEGINNER, SafetyWarningLevel.NONE,
                false, false, null, Set.of(), NAME_EN, now, now, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot have an owner");
    }

    @Test
    void assertEditableBy_rejectsNonOwner() {
        Exercise e = Exercise.createUserExercise(
                OWNER, "HANGBOARD", "FINGERS",
                DifficultyLevel.BEGINNER, SafetyWarningLevel.NONE,
                false, false, null, Set.of(), NAME_EN, null);
        assertThatThrownBy(() -> e.assertEditableBy(UUID.randomUUID()))
                .isInstanceOf(ContentOwnershipException.class);
    }

    @Test
    void softDelete_marksDeletedOnce() {
        Exercise e = Exercise.createUserExercise(
                OWNER, "HANGBOARD", "FINGERS",
                DifficultyLevel.BEGINNER, SafetyWarningLevel.NONE,
                false, false, null, Set.of(), NAME_EN, null);
        e.softDelete();
        assertThat(e.isActive()).isFalse();
        assertThat(e.getDeletedAt()).isPresent();
    }

    @Test
    void resolveField_fallsBackToSpanish() {
        Exercise e = Exercise.createUserExercise(
                OWNER, "HANGBOARD", "FINGERS",
                DifficultyLevel.BEGINNER, SafetyWarningLevel.NONE,
                false, false, null, Set.of(),
                List.of(
                        new Translation("en", Exercise.FIELD_NAME, "Hangboard"),
                        new Translation("es", Exercise.FIELD_NAME, "Tabla")),
                null);
        assertThat(e.resolveField(Exercise.FIELD_NAME, "en")).contains("Hangboard");
        assertThat(e.resolveField(Exercise.FIELD_NAME, "fr")).contains("Tabla");
    }

    @Test
    void changeVisibility_updates() {
        Exercise e = Exercise.createUserExercise(
                OWNER, "HANGBOARD", "FINGERS",
                DifficultyLevel.BEGINNER, SafetyWarningLevel.NONE,
                false, false, null, Set.of(), NAME_EN, null);
        e.changeVisibility(ContentVisibility.PUBLIC);
        assertThat(e.getVisibility()).isEqualTo(ContentVisibility.PUBLIC);
    }
}
