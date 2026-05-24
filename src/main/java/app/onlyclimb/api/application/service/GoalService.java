package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import app.onlyclimb.api.domain.exception.GoalNotFoundException;
import app.onlyclimb.api.domain.exception.InvalidGradeException;
import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.model.Goal;
import app.onlyclimb.api.domain.port.in.AchieveGoalUseCase;
import app.onlyclimb.api.domain.port.in.CreateGoalCommand;
import app.onlyclimb.api.domain.port.in.CreateGoalUseCase;
import app.onlyclimb.api.domain.port.in.DeleteGoalUseCase;
import app.onlyclimb.api.domain.port.in.GetGoalUseCase;
import app.onlyclimb.api.domain.port.in.ListGoalsQuery;
import app.onlyclimb.api.domain.port.in.ListGoalsUseCase;
import app.onlyclimb.api.domain.port.in.UpdateGoalCommand;
import app.onlyclimb.api.domain.port.in.UpdateGoalUseCase;
import app.onlyclimb.api.domain.port.out.ClimbingGradeRepository;
import app.onlyclimb.api.domain.port.out.GoalRepository;
import app.onlyclimb.api.domain.port.out.GoalRepository.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalService implements
        CreateGoalUseCase,
        UpdateGoalUseCase,
        DeleteGoalUseCase,
        AchieveGoalUseCase,
        GetGoalUseCase,
        ListGoalsUseCase {

    private final GoalRepository goalRepository;
    private final ClimbingGradeRepository gradeRepository;

    @Override
    @Transactional
    public Goal create(CreateGoalCommand command) {
        Objects.requireNonNull(command.ownerId(), "ownerId is required");
        validateGrade(command.targetGrade());
        // Supersede any currently-active goal so the partial unique index in DB
        // never trips. We deactivate (preserving history) rather than deleting.
        goalRepository.findActiveByOwner(command.ownerId()).ifPresent(active -> {
            active.deactivate();
            goalRepository.save(active);
        });
        Goal goal = Goal.create(
                command.ownerId(),
                command.type(),
                command.targetGrade(),
                command.targetDate(),
                command.notes());
        return goalRepository.save(goal);
    }

    @Override
    @Transactional
    public Goal update(UpdateGoalCommand command) {
        Goal goal = requireOwned(command.goalId(), command.callerId());
        validateGrade(command.targetGrade());
        goal.updateDetails(command.targetGrade(), command.targetDate(), command.notes());
        return goalRepository.save(goal);
    }

    @Override
    @Transactional
    public void delete(UUID goalId, UUID callerId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        try {
            goal.assertOwnedBy(callerId);
        } catch (ContentOwnershipException ex) {
            // Hide existence of goals the caller does not own.
            throw new GoalNotFoundException(goalId);
        }
        goalRepository.deleteById(goalId);
    }

    @Override
    @Transactional
    public Goal achieve(UUID goalId, UUID callerId) {
        Goal goal = requireOwned(goalId, callerId);
        goal.markAchieved(Instant.now());
        return goalRepository.save(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public Goal getOwned(UUID goalId, UUID callerId) {
        return requireOwned(goalId, callerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Goal> getCurrent(UUID callerId) {
        Objects.requireNonNull(callerId, "callerId is required");
        return goalRepository.findActiveByOwner(callerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Goal> list(ListGoalsQuery query) {
        return goalRepository.search(query);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Goal requireOwned(UUID goalId, UUID callerId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        if (callerId == null || !callerId.equals(goal.getOwnerId())) {
            throw new GoalNotFoundException(goalId);
        }
        return goal;
    }

    private void validateGrade(ClimbingGrade grade) {
        if (grade == null) return;
        if (!gradeRepository.exists(grade)) {
            throw new InvalidGradeException(grade);
        }
    }
}
