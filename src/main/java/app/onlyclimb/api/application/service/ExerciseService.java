package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.ExerciseNotFoundException;
import app.onlyclimb.api.domain.model.ContentSource;
import app.onlyclimb.api.domain.model.ContentVisibility;
import app.onlyclimb.api.domain.model.Exercise;
import app.onlyclimb.api.domain.port.in.CreateExerciseCommand;
import app.onlyclimb.api.domain.port.in.CreateExerciseUseCase;
import app.onlyclimb.api.domain.port.in.DeleteExerciseUseCase;
import app.onlyclimb.api.domain.port.in.GetExerciseUseCase;
import app.onlyclimb.api.domain.port.in.ListExercisesQuery;
import app.onlyclimb.api.domain.port.in.ListExercisesUseCase;
import app.onlyclimb.api.domain.port.in.UpdateExerciseCommand;
import app.onlyclimb.api.domain.port.in.UpdateExerciseUseCase;
import app.onlyclimb.api.domain.port.out.ExerciseRepository;
import app.onlyclimb.api.domain.port.out.ExerciseRepository.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExerciseService implements
        CreateExerciseUseCase,
        UpdateExerciseUseCase,
        DeleteExerciseUseCase,
        GetExerciseUseCase,
        ListExercisesUseCase {

    private final ExerciseRepository exerciseRepository;

    @Override
    @Transactional
    public Exercise create(CreateExerciseCommand command) {
        Exercise exercise = Exercise.createUserExercise(
                command.ownerId(),
                command.categoryCode(),
                command.primaryMuscleGroupCode(),
                command.difficultyLevel(),
                command.safetyWarningLevel(),
                command.requiresEquipment(),
                command.isUnilateral(),
                command.estimatedDurationMinutes(),
                command.allowedParameters(),
                command.translations(),
                command.visibility());
        return exerciseRepository.save(exercise);
    }

    @Override
    @Transactional
    public Exercise update(UpdateExerciseCommand command) {
        Exercise exercise = exerciseRepository.findById(command.exerciseId())
                .filter(Exercise::isActive)
                .orElseThrow(() -> new ExerciseNotFoundException(command.exerciseId()));
        exercise.assertEditableBy(command.callerId());
        exercise.updateDetails(
                command.categoryCode(),
                command.primaryMuscleGroupCode(),
                command.difficultyLevel(),
                command.safetyWarningLevel(),
                command.requiresEquipment(),
                command.isUnilateral(),
                command.estimatedDurationMinutes(),
                command.allowedParameters());
        exercise.replaceTranslations(command.translations());
        if (command.visibility() != null) {
            exercise.changeVisibility(command.visibility());
        }
        return exerciseRepository.save(exercise);
    }

    @Override
    @Transactional
    public void delete(UUID exerciseId, UUID callerId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .filter(Exercise::isActive)
                .orElseThrow(() -> new ExerciseNotFoundException(exerciseId));
        exercise.assertEditableBy(callerId);
        exercise.softDelete();
        exerciseRepository.save(exercise);
    }

    @Override
    @Transactional(readOnly = true)
    public Exercise getVisible(UUID exerciseId, UUID callerId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .filter(Exercise::isActive)
                .orElseThrow(() -> new ExerciseNotFoundException(exerciseId));
        if (isVisibleTo(exercise, callerId)) {
            return exercise;
        }
        throw new ExerciseNotFoundException(exerciseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Exercise> list(ListExercisesQuery query) {
        return exerciseRepository.search(query);
    }

    private static boolean isVisibleTo(Exercise exercise, UUID callerId) {
        if (exercise.getSource() == ContentSource.PLATFORM) {
            return true;
        }
        if (exercise.getVisibility() == ContentVisibility.PUBLIC) {
            return true;
        }
        return callerId != null
                && exercise.getOwnerId().map(o -> Objects.equals(o, callerId)).orElse(false);
    }

}
