package app.onlyclimb.api.infrastructure.adapter.out.persistence.follow;

import app.onlyclimb.api.domain.model.UserFollow;
import app.onlyclimb.api.domain.port.in.ListFollowsQuery;
import app.onlyclimb.api.domain.port.out.UserFollowRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
@Transactional
class UserFollowJpaAdapter implements UserFollowRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean follow(UUID followerId, UUID followingId) {
        Long follower = resolveUserIdOrNull(followerId);
        Long following = resolveUserIdOrNull(followingId);
        if (follower == null || following == null) {
            return false;
        }
        int rows = entityManager.createNativeQuery(
                        "INSERT INTO user_followers (follower_id, following_id) " +
                                "VALUES (:follower, :following) ON CONFLICT DO NOTHING")
                .setParameter("follower", follower)
                .setParameter("following", following)
                .executeUpdate();
        return rows > 0;
    }

    @Override
    public boolean unfollow(UUID followerId, UUID followingId) {
        Long follower = resolveUserIdOrNull(followerId);
        Long following = resolveUserIdOrNull(followingId);
        if (follower == null || following == null) {
            return false;
        }
        int rows = entityManager.createNativeQuery(
                        "DELETE FROM user_followers " +
                                "WHERE follower_id = :follower AND following_id = :following")
                .setParameter("follower", follower)
                .setParameter("following", following)
                .executeUpdate();
        return rows > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followingId) {
        Long follower = resolveUserIdOrNull(followerId);
        Long following = resolveUserIdOrNull(followingId);
        if (follower == null || following == null) {
            return false;
        }
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM user_followers " +
                                "WHERE follower_id = :follower AND following_id = :following")
                .setParameter("follower", follower)
                .setParameter("following", following)
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countFollowers(UUID userId) {
        Long id = resolveUserIdOrNull(userId);
        if (id == null) return 0L;
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM user_followers WHERE following_id = :id")
                .setParameter("id", id)
                .getSingleResult();
        return count.longValue();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFollowing(UUID userId) {
        Long id = resolveUserIdOrNull(userId);
        if (id == null) return 0L;
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM user_followers WHERE follower_id = :id")
                .setParameter("id", id)
                .getSingleResult();
        return count.longValue();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserFollow> findFollowers(ListFollowsQuery query) {
        // Followers of target: rows where following_id = target. The "other side" is follower_id.
        return findEdges(query, "following_id", "follower_id", /*reverse=*/ true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserFollow> findFollowing(ListFollowsQuery query) {
        // Users that target follows: rows where follower_id = target. The "other side" is following_id.
        return findEdges(query, "follower_id", "following_id", /*reverse=*/ false);
    }

    /**
     * Generic keyset pagination over {@code user_followers}.
     *
     * @param anchorColumn column matched against the target user (the "fixed" side)
     * @param otherColumn  column projecting the other side of the edge
     * @param reverse      when {@code true} the projected edge is {@code (other -> target)},
     *                     otherwise {@code (target -> other)}
     */
    private Page<UserFollow> findEdges(
            ListFollowsQuery query, String anchorColumn, String otherColumn, boolean reverse) {

        Long targetId = resolveUserIdOrNull(query.targetUserId());
        if (targetId == null) {
            return new Page<>(List.of(), null);
        }

        Cursor cursor = decodeCursor(query.cursor());
        StringBuilder sql = new StringBuilder()
                .append("SELECT u.uuid, uf.created_at, u.id ")
                .append("FROM user_followers uf ")
                .append("JOIN users u ON u.id = uf.").append(otherColumn).append(' ')
                .append("WHERE uf.").append(anchorColumn).append(" = :target ");
        if (cursor != null) {
            sql.append("AND (uf.created_at < :cursorAt ")
                    .append("OR (uf.created_at = :cursorAt AND u.id < :cursorId)) ");
        }
        sql.append("ORDER BY uf.created_at DESC, u.id DESC LIMIT :limit");

        var nativeQuery = entityManager.createNativeQuery(sql.toString())
                .setParameter("target", targetId)
                .setParameter("limit", query.limit() + 1);
        if (cursor != null) {
            nativeQuery.setParameter("cursorAt", Timestamp.from(cursor.createdAt));
            nativeQuery.setParameter("cursorId", cursor.otherId);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = nativeQuery.getResultList();

        String nextCursor = null;
        if (rows.size() > query.limit()) {
            Object[] last = rows.get(query.limit() - 1);
            nextCursor = encodeCursor(toInstant(last[1]), ((Number) last[2]).longValue());
            rows = rows.subList(0, query.limit());
        }

        List<UserFollow> items = new ArrayList<>(rows.size());
        UUID targetUuid = query.targetUserId();
        for (Object[] row : rows) {
            UUID otherUuid = (UUID) row[0];
            Instant createdAt = toInstant(row[1]);
            UserFollow edge = reverse
                    ? new UserFollow(otherUuid, targetUuid, createdAt)
                    : new UserFollow(targetUuid, otherUuid, createdAt);
            items.add(edge);
        }
        return new Page<>(items, nextCursor);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Long resolveUserIdOrNull(UUID uuid) {
        try {
            Object id = entityManager.createNativeQuery("SELECT id FROM users WHERE uuid = :uuid")
                    .setParameter("uuid", uuid)
                    .getSingleResult();
            return ((Number) id).longValue();
        } catch (NoResultException ex) {
            return null;
        }
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof Instant i) {
            return i;
        }
        throw new IllegalStateException("Unexpected timestamp type: " + value.getClass());
    }

    private record Cursor(Instant createdAt, long otherId) {}

    private static String encodeCursor(Instant createdAt, long otherId) {
        String raw = createdAt.toEpochMilli() + ":" + otherId;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decodeCursor(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            int sep = raw.indexOf(':');
            if (sep < 0) return null;
            Instant at = Instant.ofEpochMilli(Long.parseLong(raw.substring(0, sep)));
            long id = Long.parseLong(raw.substring(sep + 1));
            return new Cursor(at, id);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
