package app.onlyclimb.api.application.service;

import app.onlyclimb.api.domain.exception.GoalNotFoundException;
import app.onlyclimb.api.domain.exception.InvalidGradeException;
import app.onlyclimb.api.domain.model.ClimbingGrade;
import app.onlyclimb.api.domain.model.Goal;
import app.onlyclimb.api.domain.model.GoalType;
import app.onlyclimb.api.domain.model.GradeScale;
import app.onlyclimb.api.domain.port.in.CreateGoalCommand;
import app.onlyclimb.api.domain.port.in.UpdateGoalCommand;
import app.onlyclimb.api.domain.port.out.ClimbingGradeRepository;
import app.onlyclimb.api.domain.port.out.GoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();

    @Mock GoalRepository goalRepository;
    @Mock ClimbingGradeRepository gradeRepository;
    @InjectMocks GoalService service;

    @Test
    void create_withNoPreviousActive_savesNewGoal() {
        given(goalRepository.findActiveByOwner(OWNER)).willReturn(Optional.empty());
        given(goalRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        Goal created = service.create(new CreateGoalCommand(
                OWNER, GoalType.FINGER_STRENGTH, null, null, "go"));

        assertThat(created.isActive()).isTrue();
        verify(goalRepository, times(1)).save(any(Goal.class));
    }

    @Test
    void create_supersedesPreviousActiveGoal() {
        Goal previous = Goal.create(OWNER, GoalType.AEROBIC_BASE, null, null, "old");
        given(goalRepository.findActiveByOwner(OWNER)).willReturn(Optional.of(previous));
        given(goalRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        Goal created = service.create(new CreateGoalCommand(
                OWNER, GoalType.FINGER_STRENGTH, null, null, "new"));

        assertThat(previous.isActive()).isFalse();
        assertThat(created.isActive()).isTrue();
        // Two saves: the deactivated previous + the new one.
        verify(goalRepository, times(2)).save(any(Goal.class));
    }

    @Test
    void create_withUnknownGrade_throws() {
        ClimbingGrade bogus = new ClimbingGrade(GradeScale.FRENCH, "99z");
        given(gradeRepository.exists(bogus)).willReturn(false);

        assertThatThrownBy(() -> service.create(new CreateGoalCommand(
                OWNER, GoalType.GRADE_TARGET, bogus, null, null)))
                .isInstanceOf(InvalidGradeException.class);
        verify(goalRepository, never()).save(any());
    }

    @Test
    void create_withKnownGrade_passes() {
        ClimbingGrade ok = new ClimbingGrade(GradeScale.FRENCH, "7a");
        given(gradeRepository.exists(ok)).willReturn(true);
        given(goalRepository.findActiveByOwner(OWNER)).willReturn(Optional.empty());
        given(goalRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        Goal g = service.create(new CreateGoalCommand(
                OWNER, GoalType.GRADE_TARGET, ok, null, null));

        assertThat(g.getTargetGrade()).contains(ok);
    }

    @Test
    void update_strangerGetsNotFound() {
        Goal g = Goal.create(OWNER, GoalType.FINGER_STRENGTH, null, null, null);
        given(goalRepository.findById(g.getId())).willReturn(Optional.of(g));

        assertThatThrownBy(() -> service.update(new UpdateGoalCommand(
                g.getId(), STRANGER, null, null, "x")))
                .isInstanceOf(GoalNotFoundException.class);
        verify(goalRepository, never()).save(any());
    }

    @Test
    void delete_strangerGetsNotFound() {
        Goal g = Goal.create(OWNER, GoalType.FINGER_STRENGTH, null, null, null);
        given(goalRepository.findById(g.getId())).willReturn(Optional.of(g));

        assertThatThrownBy(() -> service.delete(g.getId(), STRANGER))
                .isInstanceOf(GoalNotFoundException.class);
        verify(goalRepository, never()).deleteById(any());
    }

    @Test
    void delete_owner_invokesRepo() {
        Goal g = Goal.create(OWNER, GoalType.FINGER_STRENGTH, null, null, null);
        given(goalRepository.findById(g.getId())).willReturn(Optional.of(g));

        service.delete(g.getId(), OWNER);

        verify(goalRepository).deleteById(g.getId());
    }

    @Test
    void achieve_marksAchieved_andSaves() {
        Goal g = Goal.create(OWNER, GoalType.FINGER_STRENGTH, null, null, null);
        given(goalRepository.findById(g.getId())).willReturn(Optional.of(g));
        given(goalRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        Goal result = service.achieve(g.getId(), OWNER);

        assertThat(result.isActive()).isFalse();
        assertThat(result.getAchievedAt()).isPresent();
        verify(goalRepository).save(g);
    }

    @Test
    void getCurrent_delegatesToRepo() {
        Goal active = Goal.create(OWNER, GoalType.POWER_ENDURANCE, null, null, null);
        given(goalRepository.findActiveByOwner(OWNER)).willReturn(Optional.of(active));

        Optional<Goal> result = service.getCurrent(OWNER);

        assertThat(result).contains(active);
    }
}
