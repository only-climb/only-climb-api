package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.InvalidGradeException;
import app.onlyclimb.api.domain.exception.TrainingPlanNotFoundException;
import app.onlyclimb.api.domain.exception.WorkoutTemplateNotFoundException;
import app.onlyclimb.api.domain.model.ClimbingDiscipline;
import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.DifficultyLevel;
import app.onlyclimb.api.domain.model.GoalType;
import app.onlyclimb.api.domain.model.GradeScale;
import app.onlyclimb.api.domain.model.TrainingPlan;
import app.onlyclimb.api.domain.model.TrainingPlanSession;
import app.onlyclimb.api.domain.model.TrainingPlanWeek;
import app.onlyclimb.api.domain.model.TrainingVolume;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.model.WorkoutTemplate;
import app.onlyclimb.api.domain.model.WorkoutTemplateExercise;
import app.onlyclimb.api.domain.port.in.CreateTrainingPlanCommand;
import app.onlyclimb.api.domain.port.in.EquipmentRequirementInput;
import app.onlyclimb.api.domain.port.in.TrainingPlanSessionInput;
import app.onlyclimb.api.domain.port.in.TrainingPlanWeekInput;
import app.onlyclimb.api.domain.port.out.ClimbingGradeRepository;
import app.onlyclimb.api.domain.port.out.EquipmentCatalogRepository;
import app.onlyclimb.api.domain.port.out.TrainingPlanRepository;
import app.onlyclimb.api.domain.port.out.WorkoutTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final List<Translation> NAME_ES = List.of(
            new Translation("es", TrainingPlan.FIELD_NAME, "Plan"));

    @Mock TrainingPlanRepository planRepository;
    @Mock WorkoutTemplateRepository templateRepository;
    @Mock EquipmentCatalogRepository equipmentCatalog;
    @Mock ClimbingGradeRepository climbingGradeRepository;
    @InjectMocks TrainingPlanService service;

    private WorkoutTemplate publicTemplate(UUID owner) {
        return WorkoutTemplate.createUserTemplate(
                owner, ContentVisibility.PUBLIC, DifficultyLevel.BEGINNER, null, null,
                List.of(new WorkoutTemplateExercise(1, UUID.randomUUID(),
                        java.util.Map.of(), List.of())),
                List.of(new Translation("es", WorkoutTemplate.FIELD_NAME, "T")));
    }

    private WorkoutTemplate privateTemplate(UUID owner) {
        return WorkoutTemplate.createUserTemplate(
                owner, ContentVisibility.PRIVATE, DifficultyLevel.BEGINNER, null, null,
                List.of(new WorkoutTemplateExercise(1, UUID.randomUUID(),
                        java.util.Map.of(), List.of())),
                List.of(new Translation("es", WorkoutTemplate.FIELD_NAME, "T")));
    }

    private CreateTrainingPlanCommand createCommand(
            UUID templateId,
            List<EquipmentRequirementInput> equipment,
            ClimbingGrade gradeMin) {
        return new CreateTrainingPlanCommand(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                gradeMin, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                equipment,
                List.of(new TrainingPlanWeekInput(1, false,
                        List.of(new TrainingPlanSessionInput(
                                2, 1, templateId, false, List.of())),
                        List.of())),
                NAME_ES);
    }

    @Test
    void createSavesValidPlan() {
        WorkoutTemplate template = publicTemplate(STRANGER);
        given(templateRepository.findById(template.getId())).willReturn(Optional.of(template));
        given(equipmentCatalog.findMissing(anySet())).willReturn(Set.of());
        given(planRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        TrainingPlan saved = service.create(createCommand(
                template.getId(),
                List.of(new EquipmentRequirementInput("HANGBOARD", false)),
                null));

        assertThat(saved.getOwnerId()).contains(OWNER);
        assertThat(saved.getWeeks()).hasSize(1);
        verify(planRepository).save(any(TrainingPlan.class));
    }

    @Test
    void createRejectsUnknownEquipmentCode() {
        WorkoutTemplate template = publicTemplate(STRANGER);
        given(equipmentCatalog.findMissing(anySet())).willReturn(Set.of("UFO"));

        assertThatThrownBy(() -> service.create(createCommand(
                template.getId(),
                List.of(new EquipmentRequirementInput("UFO", false)),
                null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UFO");
        verify(planRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownGrade() {
        ClimbingGrade grade = new ClimbingGrade(GradeScale.FRENCH, "9z");
        given(climbingGradeRepository.exists(grade)).willReturn(false);

        assertThatThrownBy(() -> service.create(createCommand(
                UUID.randomUUID(), List.of(), grade)))
                .isInstanceOf(InvalidGradeException.class);
        verify(planRepository, never()).save(any());
    }

    @Test
    void createRejectsTemplateInvisibleToCaller() {
        WorkoutTemplate hidden = privateTemplate(STRANGER);
        given(templateRepository.findById(hidden.getId())).willReturn(Optional.of(hidden));

        assertThatThrownBy(() -> service.create(createCommand(
                hidden.getId(), List.of(), null)))
                .isInstanceOf(WorkoutTemplateNotFoundException.class);
        verify(planRepository, never()).save(any());
    }

    @Test
    void createRejectsMissingTemplate() {
        UUID missing = UUID.randomUUID();
        given(templateRepository.findById(missing)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(createCommand(missing, List.of(), null)))
                .isInstanceOf(WorkoutTemplateNotFoundException.class);
    }

    @Test
    void getReturnsPlanWhenVisible() {
        TrainingPlan plan = TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(), NAME_ES);
        given(planRepository.findById(plan.getId())).willReturn(Optional.of(plan));

        TrainingPlan got = service.get(plan.getId(), OWNER);
        assertThat(got).isSameAs(plan);
    }

    @Test
    void getThrowsNotFoundWhenPrivateToStranger() {
        TrainingPlan plan = TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(), NAME_ES);
        given(planRepository.findById(plan.getId())).willReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.get(plan.getId(), STRANGER))
                .isInstanceOf(TrainingPlanNotFoundException.class);
    }

    @Test
    void getThrowsNotFoundForUnknownId() {
        UUID missing = UUID.randomUUID();
        given(planRepository.findById(missing)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(missing, OWNER))
                .isInstanceOf(TrainingPlanNotFoundException.class);
    }

    @Test
    void deleteRejectsNonOwner() {
        TrainingPlan plan = TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PRIVATE, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(), NAME_ES);
        given(planRepository.findById(plan.getId())).willReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.delete(plan.getId(), STRANGER))
                .isInstanceOf(ContentOwnershipException.class);
        verify(planRepository, never()).save(any());
    }

    @Test
    void forkProducesIndependentCopy() {
        TrainingPlan source = TrainingPlan.createUserPlan(
                OWNER, ContentVisibility.PUBLIC, DifficultyLevel.INTERMEDIATE,
                ClimbingDiscipline.SPORT, GoalType.FINGER_STRENGTH, Set.of(),
                null, null, 4, 3, 60, TrainingVolume.MODERATE,
                true, false, false, false, false,
                Set.of(), List.of(), NAME_ES);
        given(planRepository.findById(source.getId())).willReturn(Optional.of(source));
        given(planRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        TrainingPlan fork = service.fork(source.getId(), STRANGER);

        assertThat(fork.getOwnerId()).contains(STRANGER);
        assertThat(fork.getForkedFromId()).contains(source.getId());
        assertThat(fork.getVisibility()).isEqualTo(ContentVisibility.PRIVATE);
        assertThat(fork.getSource()).isEqualTo(ContentSource.USER_CREATED);
    }
}
