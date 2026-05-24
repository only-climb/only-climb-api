package app.onlyclimb.api.domain.model;

import app.onlyclimb.api.domain.exception.CannotFollowSelfException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserFollowTest {

    @Test
    void buildsValidEdge() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Instant now = Instant.now();
        UserFollow edge = new UserFollow(a, b, now);
        assertThat(edge.followerId()).isEqualTo(a);
        assertThat(edge.followingId()).isEqualTo(b);
        assertThat(edge.createdAt()).isEqualTo(now);
    }

    @Test
    void rejectsSelfFollow() {
        UUID a = UUID.randomUUID();
        assertThatThrownBy(() -> new UserFollow(a, a, Instant.now()))
                .isInstanceOf(CannotFollowSelfException.class);
    }

    @Test
    void rejectsNulls() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertThatThrownBy(() -> new UserFollow(null, b, Instant.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UserFollow(a, null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UserFollow(a, b, null))
                .isInstanceOf(NullPointerException.class);
    }
}
