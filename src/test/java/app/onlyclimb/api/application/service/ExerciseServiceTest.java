package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.ExerciseNotFoundException;
import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.Exercise;
import app.onlyclimb.api.domain.model.SafetyWarningLevel;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.port.in.CreateExerciseCommand;
import app.onlyclimb.api.domain.port.in.ListExercisesQuery;
import app.onlyclimb.api.domain.port.in.UpdateExerciseCommand;
import app.onlyclimb.api.domain.port.out.ExerciseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final List<Translation> NAME_EN = List.of(
            new Translation("en", Exercise.FIELD_NAME, "Hangboard repeaters"));

    @Mock ExerciseRepository repository;
    @InjectMocks ExerciseService service;

    private Exercise userExercise;

    @BeforeEach
    void setUp() {
        userExercise = Exercise.createUserExercise(
                OWNER, "HANGBOARD", "FINGERS",
                DifficultyLevel.INTERMEDIATE, SafetyWarningLevel.MODERATE,
                true, false, 20, Set.of(), NAME_EN, ContentVisibility.PRIVATE);
    }

    @Test
    void create_callsRepository() {
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
        Exercise result = service.create(new CreateExerciseCommand(
                OWNER, "HANGBOARD", "FINGERS",
                DifficultyLevel.BEGINNER, SafetyWarningLevel.NONE,
                false, false, null, Set.of(), NAME_EN, ContentVisibility.PRIVATE));
        assertThat(result.getOwnerId()).contains(OWNER);
        verify(repository).save(any(Exercise.class));
    }

    @Test
    void update_rejectsNonOwner() {
        given(repository.findById(userExercise.getId())).willReturn(Optional.of(userExercise));
        UpdateExerciseCommand cmd = new UpdateExerciseCommand(
                userExercise.getId(), STRANGER,
                "HANGBOARD", "FINGERS",
                DifficultyLevel.INTERMEDIATE, SafetyWarningLevel.NONE,
                false, false, null, Set.of(), NAME_EN, null);
        assertThatThrownBy(() -> service.update(cmd))
                .isInstanceOf(ContentOwnershipException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_persistsChanges() {
        given(repository.findById(userExercise.getId())).willReturn(Optional.of(userExercise));
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
        UpdateExerciseCommand cmd = new UpdateExerciseCommand(
                userExercise.getId(), OWNER,
                "PULL", "BACK",
                DifficultyLevel.ADVANCED, SafetyWarningLevel.HIGH,
                false, true, 30, Set.of(), NAME_EN, ContentVisibility.PUBLIC);
        Exercise updated = service.update(cmd);
        assertThat(updated.getCategoryCode()).isEqualTo("PULL");
        assertThat(updated.getVisibility()).isEqualTo(ContentVisibility.PUBLIC);
    }

    @Test
    void delete_rejectsNonOwner() {
        given(repository.findById(userExercise.getId())).willReturn(Optional.of(userExercise));
        assertThatThrownBy(() -> service.delete(userExercise.getId(), STRANGER))
                .isInstanceOf(ContentOwnershipException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void delete_softDeletes() {
        given(repository.findById(userExercise.getId())).willReturn(Optional.of(userExercise));
        service.delete(userExercise.getId(), OWNER);
        assertThat(userExercise.isActive()).isFalse();
        verify(repository).save(userExercise);
    }

    @Test
    void getVisible_hidesPrivateFromStranger() {
        given(repository.findById(userExercise.getId())).willReturn(Optional.of(userExercise));
        assertThatThrownBy(() -> service.getVisible(userExercise.getId(), STRANGER))
                .isInstanceOf(ExerciseNotFoundException.class);
    }

    @Test
    void getVisible_allowsOwner() {
        given(repository.findById(userExercise.getId())).willReturn(Optional.of(userExercise));
        Exercise result = service.getVisible(userExercise.getId(), OWNER);
        assertThat(result).isSameAs(userExercise);
    }

    @Test
    void getVisible_allowsPlatformAnonymously() {
        Instant now = Instant.now();
        Exercise platform = new Exercise(
                UUID.randomUUID(), ContentSource.PLATFORM, null,
                "HANGBOARD", "FINGERS",
                ContentVisibility.PUBLIC, DifficultyLevel.BEGINNER, SafetyWarningLevel.NONE,
                false, false, null, Set.of(), NAME_EN, now, now, null);
        given(repository.findById(platform.getId())).willReturn(Optional.of(platform));
        Exercise result = service.getVisible(platform.getId(), null);
        assertThat(result).isSameAs(platform);
    }

    @Test
    void list_passesQueryThrough() {
        ListExercisesQuery q = new ListExercisesQuery(null, null, null, false, null, 20);
        given(repository.search(q)).willReturn(new ExerciseRepository.Page<>(List.of(), null));
        var page = service.list(q);
        assertThat(page.items()).isEmpty();
        verify(repository).search(q);
    }
}
