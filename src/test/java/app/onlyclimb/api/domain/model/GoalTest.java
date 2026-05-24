package app.onlyclimb.api.domain.model;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoalTest {

    private static final UUID OWNER = UUID.randomUUID();

    @Test
    void create_setsActiveAndTimestamps() {
        Goal g = Goal.create(OWNER, GoalType.FINGER_STRENGTH, null, null, "go");

        assertThat(g.getId()).isNotNull();
        assertThat(g.getOwnerId()).isEqualTo(OWNER);
        assertThat(g.isActive()).isTrue();
        assertThat(g.getAchievedAt()).isEmpty();
        assertThat(g.getTargetGrade()).isEmpty();
        assertThat(g.getNotes()).contains("go");
        assertThat(g.getCreatedAt()).isNotNull();
        assertThat(g.getUpdatedAt()).isEqualTo(g.getCreatedAt());
    }

    @Test
    void create_gradeTarget_requiresGrade() {
        assertThatThrownBy(() -> Goal.create(OWNER, GoalType.GRADE_TARGET, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a target grade");
    }

    @Test
    void create_nonGradeType_rejectsGrade() {
        ClimbingGrade g = new ClimbingGrade(GradeScale.FRENCH, "7a");
        assertThatThrownBy(() -> Goal.create(OWNER, GoalType.AEROBIC_BASE, g, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not accept");
    }

    @Test
    void markAchieved_deactivatesAndStampsTime() throws InterruptedException {
        Goal g = Goal.create(OWNER, GoalType.GRADE_TARGET,
                new ClimbingGrade(GradeScale.FRENCH, "7a"), LocalDate.now(), null);
        Instant before = g.getUpdatedAt();
        Thread.sleep(2);

        g.markAchieved(Instant.parse("2025-02-01T00:00:00Z"));

        assertThat(g.isActive()).isFalse();
        assertThat(g.getAchievedAt()).contains(Instant.parse("2025-02-01T00:00:00Z"));
        assertThat(g.getUpdatedAt()).isAfter(before);
    }

    @Test
    void markAchieved_twice_throws() {
        Goal g = Goal.create(OWNER, GoalType.FINGER_STRENGTH, null, null, null);
        g.markAchieved(Instant.now());
        assertThatThrownBy(() -> g.markAchieved(Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deactivate_isIdempotent() {
        Goal g = Goal.create(OWNER, GoalType.FINGER_STRENGTH, null, null, null);
        g.deactivate();
        assertThat(g.isActive()).isFalse();
        // second call is a no-op
        g.deactivate();
        assertThat(g.isActive()).isFalse();
        assertThat(g.getAchievedAt()).isEmpty();
    }

    @Test
    void assertOwnedBy_strangerThrows() {
        Goal g = Goal.create(OWNER, GoalType.FINGER_STRENGTH, null, null, null);
        assertThatThrownBy(() -> g.assertOwnedBy(UUID.randomUUID()))
                .isInstanceOf(ContentOwnershipException.class);
    }

    @Test
    void climbingGrade_equalityIsValueBased() {
        ClimbingGrade a = new ClimbingGrade(GradeScale.FRENCH, "7a");
        ClimbingGrade b = new ClimbingGrade(GradeScale.FRENCH, "7a");
        ClimbingGrade c = new ClimbingGrade(GradeScale.FONTAINEBLEAU, "7a");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void climbingGrade_rejectsBlankValue() {
        assertThatThrownBy(() -> new ClimbingGrade(GradeScale.FRENCH, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
