package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.ExerciseNotFoundException;
import app.onlyclimb.api.domain.exception.InvalidExerciseConfigException;
import app.onlyclimb.api.domain.exception.WorkoutTemplateNotFoundException;
import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.Exercise;
import app.onlyclimb.api.domain.model.ParameterType;
import app.onlyclimb.api.domain.model.WorkoutTemplate;
import app.onlyclimb.api.domain.model.WorkoutTemplateExercise;
import app.onlyclimb.api.domain.port.in.CreateWorkoutTemplateCommand;
import app.onlyclimb.api.domain.port.in.CreateWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.DeleteWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.ForkWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.GetWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.ListWorkoutTemplatesQuery;
import app.onlyclimb.api.domain.port.in.ListWorkoutTemplatesUseCase;
import app.onlyclimb.api.domain.port.in.UpdateWorkoutTemplateCommand;
import app.onlyclimb.api.domain.port.in.UpdateWorkoutTemplateUseCase;
import app.onlyclimb.api.domain.port.in.WorkoutTemplateExerciseEntry;
import app.onlyclimb.api.domain.port.out.ExerciseRepository;
import app.onlyclimb.api.domain.port.out.WorkoutTemplateRepository;
import app.onlyclimb.api.domain.port.out.WorkoutTemplateRepository.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutTemplateService implements
        CreateWorkoutTemplateUseCase,
        UpdateWorkoutTemplateUseCase,
        DeleteWorkoutTemplateUseCase,
        GetWorkoutTemplateUseCase,
        ListWorkoutTemplatesUseCase,
        ForkWorkoutTemplateUseCase {

    private final WorkoutTemplateRepository templateRepository;
    private final ExerciseRepository exerciseRepository;

    @Override
    @Transactional
    public WorkoutTemplate create(CreateWorkoutTemplateCommand command) {
        List<WorkoutTemplateExercise> entries = mapAndValidateEntries(
                command.exercises(), command.ownerId());
        WorkoutTemplate template = WorkoutTemplate.createUserTemplate(
                command.ownerId(),
                command.visibility(),
                command.difficultyLevel(),
                command.estimatedDurationMinutes(),
                command.targetDiscipline(),
                entries,
                command.translations());
        return templateRepository.save(template);
    }

    @Override
    @Transactional
    public WorkoutTemplate update(UpdateWorkoutTemplateCommand command) {
        WorkoutTemplate template = templateRepository.findById(command.templateId())
                .filter(WorkoutTemplate::isActive)
                .orElseThrow(() -> new WorkoutTemplateNotFoundException(command.templateId()));
        template.assertEditableBy(command.callerId());
        List<WorkoutTemplateExercise> entries = mapAndValidateEntries(
                command.exercises(), command.callerId());
        template.updateDetails(
                command.difficultyLevel(),
                command.estimatedDurationMinutes(),
                command.targetDiscipline());
        template.replaceExercises(entries);
        template.replaceTranslations(command.translations());
        if (command.visibility() != null) {
            template.changeVisibility(command.visibility());
        }
        return templateRepository.save(template);
    }

    @Override
    @Transactional
    public void delete(UUID templateId, UUID callerId) {
        WorkoutTemplate template = templateRepository.findById(templateId)
                .filter(WorkoutTemplate::isActive)
                .orElseThrow(() -> new WorkoutTemplateNotFoundException(templateId));
        template.assertEditableBy(callerId);
        template.softDelete();
        templateRepository.save(template);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkoutTemplate getVisible(UUID templateId, UUID callerId) {
        WorkoutTemplate template = templateRepository.findById(templateId)
                .filter(WorkoutTemplate::isActive)
                .orElseThrow(() -> new WorkoutTemplateNotFoundException(templateId));
        if (isVisibleTo(template, callerId)) {
            return template;
        }
        throw new WorkoutTemplateNotFoundException(templateId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkoutTemplate> list(ListWorkoutTemplatesQuery query) {
        return templateRepository.search(query);
    }

    @Override
    @Transactional
    public WorkoutTemplate fork(UUID sourceTemplateId, UUID callerId) {
        Objects.requireNonNull(callerId, "callerId is required");
        WorkoutTemplate source = templateRepository.findById(sourceTemplateId)
                .filter(WorkoutTemplate::isActive)
                .orElseThrow(() -> new WorkoutTemplateNotFoundException(sourceTemplateId));
        if (!isVisibleTo(source, callerId)) {
            throw new WorkoutTemplateNotFoundException(sourceTemplateId);
        }
        WorkoutTemplate copy = source.fork(callerId);
        return templateRepository.save(copy);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private List<WorkoutTemplateExercise> mapAndValidateEntries(
            List<WorkoutTemplateExerciseEntry> input, UUID callerId) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Template must contain at least one exercise");
        }
        List<WorkoutTemplateExercise> result = new ArrayList<>(input.size());
        for (WorkoutTemplateExerciseEntry entry : input) {
            Exercise exercise = exerciseRepository.findById(entry.exerciseId())
                    .filter(Exercise::isActive)
                    .orElseThrow(() -> new ExerciseNotFoundException(entry.exerciseId()));
            if (!isExerciseVisibleTo(exercise, callerId)) {
                throw new ExerciseNotFoundException(entry.exerciseId());
            }
            validateConfigKeys(exercise, entry.config());
            result.add(new WorkoutTemplateExercise(
                    entry.position(),
                    entry.exerciseId(),
                    entry.config(),
                    entry.notesTranslations()));
        }
        return result;
    }

    private static void validateConfigKeys(Exercise exercise, java.util.Map<ParameterType, String> config) {
        if (config == null || config.isEmpty()) return;
        Set<ParameterType> allowed = exercise.getAllowedParameters();
        Set<ParameterType> unknown = new HashSet<>(config.keySet());
        unknown.removeAll(allowed);
        if (!unknown.isEmpty()) {
            throw new InvalidExerciseConfigException(
                    "Exercise " + exercise.getId() + " does not allow parameters: " + unknown);
        }
    }

    private static boolean isVisibleTo(WorkoutTemplate template, UUID callerId) {
        if (template.getSource() == ContentSource.PLATFORM) return true;
        if (template.getVisibility() == ContentVisibility.PUBLIC) return true;
        return callerId != null
                && template.getOwnerId().map(o -> Objects.equals(o, callerId)).orElse(false);
    }

    private static boolean isExerciseVisibleTo(Exercise exercise, UUID callerId) {
        if (exercise.getSource() == ContentSource.PLATFORM) return true;
        if (exercise.getVisibility() == ContentVisibility.PUBLIC) return true;
        return callerId != null
                && exercise.getOwnerId().map(o -> Objects.equals(o, callerId)).orElse(false);
    }
}
