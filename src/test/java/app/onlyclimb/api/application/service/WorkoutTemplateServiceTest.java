package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.ExerciseNotFoundException;
import app.onlyclimb.api.domain.exception.InvalidExerciseConfigException;
import app.onlyclimb.api.domain.exception.WorkoutTemplateNotFoundException;
import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.Exercise;
import app.onlyclimb.api.domain.model.ParameterType;
import app.onlyclimb.api.domain.model.SafetyWarningLevel;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.model.WorkoutTemplate;
import app.onlyclimb.api.domain.port.in.CreateWorkoutTemplateCommand;
import app.onlyclimb.api.domain.port.in.UpdateWorkoutTemplateCommand;
import app.onlyclimb.api.domain.port.in.WorkoutTemplateExerciseEntry;
import app.onlyclimb.api.domain.port.out.ExerciseRepository;
import app.onlyclimb.api.domain.port.out.WorkoutTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
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
class WorkoutTemplateServiceTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final List<Translation> NAME_EN = List.of(
            new Translation("en", WorkoutTemplate.FIELD_NAME, "Session 1"));

    @Mock WorkoutTemplateRepository templateRepository;
    @Mock ExerciseRepository exerciseRepository;
    @InjectMocks WorkoutTemplateService service;

    private Exercise publicExercise;
    private Exercise restrictedExercise;

    @BeforeEach
    void setUp() {
        publicExercise = Exercise.createUserExercise(
                OWNER, "HANGBOARD", "FINGERS",
                DifficultyLevel.INTERMEDIATE, SafetyWarningLevel.MODERATE,
                true, false, null,
                Set.of(ParameterType.REPS, ParameterType.SETS),
                List.of(new Translation("en", Exercise.FIELD_NAME, "Repeaters")),
                ContentVisibility.PUBLIC);
        restrictedExercise = Exercise.createUserExercise(
                STRANGER, "HANGBOARD", "FINGERS",
                DifficultyLevel.INTERMEDIATE, SafetyWarningLevel.MODERATE,
                true, false, null,
                Set.of(ParameterType.REPS),
                List.of(new Translation("en", Exercise.FIELD_NAME, "Secret")),
                ContentVisibility.PRIVATE);
    }

    private WorkoutTemplateExerciseEntry validEntry() {
        return new WorkoutTemplateExerciseEntry(
                1, publicExercise.getId(),
                Map.of(ParameterType.REPS, "5", ParameterType.SETS, "3"),
                List.of());
    }

    @Test
    void create_validatesEntries_andSaves() {
        given(exerciseRepository.findById(publicExercise.getId())).willReturn(Optional.of(publicExercise));
        given(templateRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        WorkoutTemplate result = service.create(new CreateWorkoutTemplateCommand(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.BEGINNER, null, null,
                List.of(validEntry()), NAME_EN));

        assertThat(result.getOwnerId()).contains(OWNER);
        assertThat(result.getExercises()).hasSize(1);
        verify(templateRepository).save(any(WorkoutTemplate.class));
    }

    @Test
    void create_rejectsUnknownConfigKey() {
        given(exerciseRepository.findById(publicExercise.getId())).willReturn(Optional.of(publicExercise));

        WorkoutTemplateExerciseEntry bad = new WorkoutTemplateExerciseEntry(
                1, publicExercise.getId(),
                Map.of(ParameterType.WEIGHT_KG, "20"),  // not in allowed
                List.of());

        assertThatThrownBy(() -> service.create(new CreateWorkoutTemplateCommand(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.BEGINNER, null, null,
                List.of(bad), NAME_EN)))
                .isInstanceOf(InvalidExerciseConfigException.class)
                .hasMessageContaining("WEIGHT_KG");
        verify(templateRepository, never()).save(any());
    }

    @Test
    void create_rejectsEmbeddingPrivateForeignExercise() {
        given(exerciseRepository.findById(restrictedExercise.getId()))
                .willReturn(Optional.of(restrictedExercise));

        WorkoutTemplateExerciseEntry foreign = new WorkoutTemplateExerciseEntry(
                1, restrictedExercise.getId(), Map.of(), List.of());

        assertThatThrownBy(() -> service.create(new CreateWorkoutTemplateCommand(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.BEGINNER, null, null,
                List.of(foreign), NAME_EN)))
                .isInstanceOf(ExerciseNotFoundException.class);
    }

    @Test
    void update_rejectsNonOwner() {
        WorkoutTemplate template = WorkoutTemplate.createUserTemplate(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.BEGINNER, null, null,
                List.of(new app.onlyclimb.api.domain.model.WorkoutTemplateExercise(
                        1, publicExercise.getId(), Map.of(), List.of())),
                NAME_EN);
        given(templateRepository.findById(template.getId())).willReturn(Optional.of(template));

        UpdateWorkoutTemplateCommand cmd = new UpdateWorkoutTemplateCommand(
                template.getId(), STRANGER, null, DifficultyLevel.ADVANCED, null, null,
                List.of(validEntry()), NAME_EN);

        assertThatThrownBy(() -> service.update(cmd))
                .isInstanceOf(ContentOwnershipException.class);
    }

    @Test
    void delete_softDeletes() {
        WorkoutTemplate template = WorkoutTemplate.createUserTemplate(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.BEGINNER, null, null,
                List.of(new app.onlyclimb.api.domain.model.WorkoutTemplateExercise(
                        1, publicExercise.getId(), Map.of(), List.of())),
                NAME_EN);
        given(templateRepository.findById(template.getId())).willReturn(Optional.of(template));

        service.delete(template.getId(), OWNER);

        assertThat(template.isActive()).isFalse();
        verify(templateRepository).save(template);
    }

    @Test
    void getVisible_hidesPrivateFromStranger() {
        WorkoutTemplate template = WorkoutTemplate.createUserTemplate(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.BEGINNER, null, null,
                List.of(new app.onlyclimb.api.domain.model.WorkoutTemplateExercise(
                        1, publicExercise.getId(), Map.of(), List.of())),
                NAME_EN);
        given(templateRepository.findById(template.getId())).willReturn(Optional.of(template));

        assertThatThrownBy(() -> service.getVisible(template.getId(), STRANGER))
                .isInstanceOf(WorkoutTemplateNotFoundException.class);
    }

    @Test
    void fork_copiesAsPrivateUserCreated_pointingAtSource() {
        WorkoutTemplate source = WorkoutTemplate.createUserTemplate(
                OWNER, ContentVisibility.PUBLIC, DifficultyLevel.ADVANCED, 45,
                ClimbingDiscipline.SPORT,
                List.of(new app.onlyclimb.api.domain.model.WorkoutTemplateExercise(
                        1, publicExercise.getId(), Map.of(), List.of())),
                NAME_EN);
        given(templateRepository.findById(source.getId())).willReturn(Optional.of(source));
        given(templateRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        WorkoutTemplate copy = service.fork(source.getId(), STRANGER);

        assertThat(copy.getSource()).isEqualTo(ContentSource.USER_CREATED);
        assertThat(copy.getOwnerId()).contains(STRANGER);
        assertThat(copy.getForkedFromId()).contains(source.getId());
        assertThat(copy.getVisibility()).isEqualTo(ContentVisibility.PRIVATE);
    }

    @Test
    void fork_rejectsNotVisibleSource() {
        WorkoutTemplate privateSource = WorkoutTemplate.createUserTemplate(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.BEGINNER, null, null,
                List.of(new app.onlyclimb.api.domain.model.WorkoutTemplateExercise(
                        1, publicExercise.getId(), Map.of(), List.of())),
                NAME_EN);
        given(templateRepository.findById(privateSource.getId())).willReturn(Optional.of(privateSource));

        assertThatThrownBy(() -> service.fork(privateSource.getId(), STRANGER))
                .isInstanceOf(WorkoutTemplateNotFoundException.class);
    }
}
