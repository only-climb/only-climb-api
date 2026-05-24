package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.InvalidGradeException;
import app.onlyclimb.api.domain.exception.TrainingPlanNotFoundException;
import app.onlyclimb.api.domain.exception.WorkoutTemplateNotFoundException;
import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.EquipmentRequirement;
import app.onlyclimb.api.domain.model.TrainingPlan;
import app.onlyclimb.api.domain.model.TrainingPlanSession;
import app.onlyclimb.api.domain.model.TrainingPlanWeek;
import app.onlyclimb.api.domain.model.Translation;
import app.onlyclimb.api.domain.model.WorkoutTemplate;
import app.onlyclimb.api.domain.port.in.CreateTrainingPlanCommand;
import app.onlyclimb.api.domain.port.in.CreateTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.DeleteTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.EquipmentRequirementInput;
import app.onlyclimb.api.domain.port.in.ForkTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.GetTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.in.ListTrainingPlansQuery;
import app.onlyclimb.api.domain.port.in.ListTrainingPlansUseCase;
import app.onlyclimb.api.domain.port.in.TrainingPlanSessionInput;
import app.onlyclimb.api.domain.port.in.TrainingPlanWeekInput;
import app.onlyclimb.api.domain.port.in.UpdateTrainingPlanCommand;
import app.onlyclimb.api.domain.port.in.UpdateTrainingPlanUseCase;
import app.onlyclimb.api.domain.port.out.ClimbingGradeRepository;
import app.onlyclimb.api.domain.port.out.EquipmentCatalogRepository;
import app.onlyclimb.api.domain.port.out.TrainingPlanRepository;
import app.onlyclimb.api.domain.port.out.TrainingPlanRepository.Page;
import app.onlyclimb.api.domain.port.out.WorkoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainingPlanService implements
        CreateTrainingPlanUseCase,
        UpdateTrainingPlanUseCase,
        DeleteTrainingPlanUseCase,
        GetTrainingPlanUseCase,
        ListTrainingPlansUseCase,
        ForkTrainingPlanUseCase {

    private final TrainingPlanRepository planRepository;
    private final WorkoutTemplateRepository templateRepository;
    private final EquipmentCatalogRepository equipmentCatalog;
    private final ClimbingGradeRepository climbingGradeRepository;

    @Override
    @Transactional
    public TrainingPlan create(CreateTrainingPlanCommand command) {
        Objects.requireNonNull(command, "command is required");
        validateGrade(command.targetGradeMin(), "targetGradeMin");
        validateGrade(command.targetGradeMax(), "targetGradeMax");
        Set<EquipmentRequirement> equipment = mapAndValidateEquipment(command.equipment());
        List<TrainingPlanWeek> weeks = mapAndValidateWeeks(command.weeks(), command.ownerId());
        return planRepository.save(TrainingPlan.createUserPlan(
                command.ownerId(),
                command.visibility(),
                command.difficultyLevel(),
                command.targetDiscipline(),
                command.primaryGoal(),
                command.secondaryGoals(),
                command.targetGradeMin(),
                command.targetGradeMax(),
                command.durationWeeks(),
                command.sessionsPerWeek(),
                command.avgSessionDurationMinutes(),
                command.trainingVolume(),
                command.requiresHangboard(),
                command.requiresCampusBoard(),
                command.requiresGymAccess(),
                command.requiresOutdoorClimbing(),
                command.recoveryFocused(),
                equipment,
                weeks,
                command.translations()));
    }

    @Override
    @Transactional
    public TrainingPlan update(UpdateTrainingPlanCommand command) {
        Objects.requireNonNull(command, "command is required");
        TrainingPlan plan = planRepository.findById(command.planId())
                .filter(TrainingPlan::isActive)
                .orElseThrow(() -> new TrainingPlanNotFoundException(command.planId()));
        plan.assertEditableBy(command.callerId());
        validateGrade(command.targetGradeMin(), "targetGradeMin");
        validateGrade(command.targetGradeMax(), "targetGradeMax");
        Set<EquipmentRequirement> equipment = mapAndValidateEquipment(command.equipment());
        List<TrainingPlanWeek> weeks = mapAndValidateWeeks(command.weeks(), command.callerId());

        plan.updateDetails(
                command.difficultyLevel(),
                command.targetDiscipline(),
                command.primaryGoal(),
                command.targetGradeMin(),
                command.targetGradeMax(),
                command.durationWeeks(),
                command.sessionsPerWeek(),
                command.avgSessionDurationMinutes(),
                command.trainingVolume(),
                command.requiresHangboard(),
                command.requiresCampusBoard(),
                command.requiresGymAccess(),
                command.requiresOutdoorClimbing(),
                command.recoveryFocused());
        plan.replaceSecondaryGoals(command.secondaryGoals());
        plan.replaceEquipment(equipment);
        plan.replaceWeeks(weeks);
        plan.replaceTranslations(command.translations());
        if (command.visibility() != null) {
            plan.changeVisibility(command.visibility());
        }
        return planRepository.save(plan);
    }

    @Override
    @Transactional
    public void delete(UUID planId, UUID callerId) {
        TrainingPlan plan = planRepository.findById(planId)
                .filter(TrainingPlan::isActive)
                .orElseThrow(() -> new TrainingPlanNotFoundException(planId));
        plan.assertEditableBy(callerId);
        plan.softDelete();
        planRepository.save(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingPlan get(UUID planId, UUID callerId) {
        TrainingPlan plan = planRepository.findById(planId)
                .filter(TrainingPlan::isActive)
                .orElseThrow(() -> new TrainingPlanNotFoundException(planId));
        if (isVisibleTo(plan, callerId)) {
            return plan;
        }
        throw new TrainingPlanNotFoundException(planId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingPlan> list(ListTrainingPlansQuery query) {
        return planRepository.search(query);
    }

    @Override
    @Transactional
    public TrainingPlan fork(UUID sourcePlanId, UUID callerId) {
        Objects.requireNonNull(callerId, "callerId is required");
        TrainingPlan source = planRepository.findById(sourcePlanId)
                .filter(TrainingPlan::isActive)
                .orElseThrow(() -> new TrainingPlanNotFoundException(sourcePlanId));
        if (!isVisibleTo(source, callerId)) {
            throw new TrainingPlanNotFoundException(sourcePlanId);
        }
        return planRepository.save(source.fork(callerId));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private List<TrainingPlanWeek> mapAndValidateWeeks(
            List<TrainingPlanWeekInput> input, UUID callerId) {
        if (input == null) return List.of();
        List<TrainingPlanWeek> result = new ArrayList<>(input.size());
        for (TrainingPlanWeekInput w : input) {
            List<TrainingPlanSession> sessions = mapAndValidateSessions(w.sessions(), callerId);
            result.add(new TrainingPlanWeek(
                    UUID.randomUUID(),
                    w.weekNumber(),
                    w.deload(),
                    sessions,
                    safeTranslations(w.translations())));
        }
        return result;
    }

    private List<TrainingPlanSession> mapAndValidateSessions(
            List<TrainingPlanSessionInput> input, UUID callerId) {
        if (input == null) return List.of();
        List<TrainingPlanSession> result = new ArrayList<>(input.size());
        for (TrainingPlanSessionInput s : input) {
            WorkoutTemplate template = templateRepository.findById(s.workoutTemplateId())
                    .filter(WorkoutTemplate::isActive)
                    .orElseThrow(() -> new WorkoutTemplateNotFoundException(s.workoutTemplateId()));
            if (!isTemplateVisibleTo(template, callerId)) {
                throw new WorkoutTemplateNotFoundException(s.workoutTemplateId());
            }
            result.add(new TrainingPlanSession(
                    UUID.randomUUID(),
                    s.dayOfWeek(),
                    s.position(),
                    s.workoutTemplateId(),
                    s.optional(),
                    safeTranslations(s.notesTranslations())));
        }
        return result;
    }

    private Set<EquipmentRequirement> mapAndValidateEquipment(List<EquipmentRequirementInput> input) {
        if (input == null || input.isEmpty()) return Set.of();
        Set<String> codes = new LinkedHashSet<>();
        Set<EquipmentRequirement> result = new LinkedHashSet<>();
        for (EquipmentRequirementInput e : input) {
            if (e == null) continue;
            codes.add(e.code());
            result.add(new EquipmentRequirement(e.code(), e.optional()));
        }
        Set<String> missing = equipmentCatalog.findMissing(codes);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Unknown equipment codes: " + missing);
        }
        return result;
    }

    private void validateGrade(ClimbingGrade grade, String fieldName) {
        if (grade == null) return;
        if (!climbingGradeRepository.exists(grade)) {
            throw new InvalidGradeException(
                    fieldName + " is not a valid grade in the catalog: " + grade);
        }
    }

    private static Iterable<Translation> safeTranslations(List<Translation> in) {
        return in == null ? List.of() : in;
    }

    private static boolean isVisibleTo(TrainingPlan plan, UUID callerId) {
        if (plan.getSource() == ContentSource.PLATFORM) return true;
        if (plan.getVisibility() == ContentVisibility.PUBLIC) return true;
        return callerId != null
                && plan.getOwnerId().map(o -> Objects.equals(o, callerId)).orElse(false);
    }

    private static boolean isTemplateVisibleTo(WorkoutTemplate template, UUID callerId) {
        if (template.getSource() == ContentSource.PLATFORM) return true;
        if (template.getVisibility() == ContentVisibility.PUBLIC) return true;
        return callerId != null
                && template.getOwnerId().map(o -> Objects.equals(o, callerId)).orElse(false);
    }
}
