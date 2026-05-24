package app.onlyclimb.api.domain.model;

import app.onlyclimb.api.domain.exception.ContentOwnershipException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregate root representing a structured training objective a user is
 * pursuing. At most one goal per user can be {@link #isActive active} at any
 * time (enforced by partial unique index in DB).
 *
 * <p>{@link GoalType#requiresTargetGrade()} types (today: {@link GoalType#GRADE_TARGET})
 * must always carry a non-null {@link #getTargetGrade() target grade}.</p>
 *
 * <p>Goals are strictly personal: only the {@link #getOwnerId owner} may read,
 * modify or delete them.</p>
 */
public class Goal {

    private final UUID id;
    private final UUID ownerId;
    private final GoalType type;
    private ClimbingGrade targetGrade;
    private LocalDate targetDate;
    private boolean active;
    private Instant achievedAt;
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;

    public Goal(
            UUID id,
            UUID ownerId,
            GoalType type,
            ClimbingGrade targetGrade,
            LocalDate targetDate,
            boolean active,
            Instant achievedAt,
            String notes,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId is required");
        this.type = Objects.requireNonNull(type, "type is required");
        assignGrade(targetGrade);
        this.targetDate = targetDate;
        this.active = active;
        this.achievedAt = achievedAt;
        this.notes = notes;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    /** Factory for a new active goal. */
    public static Goal create(
            UUID ownerId,
            GoalType type,
            ClimbingGrade targetGrade,
            LocalDate targetDate,
            String notes) {
        Instant now = Instant.now();
        return new Goal(
                UUID.randomUUID(),
                ownerId,
                type,
                targetGrade,
                targetDate,
                true,
                null,
                notes,
                now,
                now);
    }

    // ---------------------------------------------------------------------
    // Mutators
    // ---------------------------------------------------------------------

    public void updateDetails(ClimbingGrade targetGrade, LocalDate targetDate, String notes) {
        assignGrade(targetGrade);
        this.targetDate = targetDate;
        this.notes = notes;
        touch();
    }

    /**
     * Marks the goal as achieved. Sets {@code achievedAt} and deactivates it.
     * @throws IllegalStateException if already achieved
     */
    public void markAchieved(Instant at) {
        if (achievedAt != null) {
            throw new IllegalStateException("Goal is already achieved");
        }
        this.achievedAt = Objects.requireNonNull(at, "achievedAt is required");
        this.active = false;
        touch();
    }

    /** Deactivates the goal without marking it as achieved (giving up / superseded). */
    public void deactivate() {
        if (!active) return;
        this.active = false;
        touch();
    }

    public void assertOwnedBy(UUID callerId) {
        if (callerId == null || !callerId.equals(ownerId)) {
            throw new ContentOwnershipException("Caller does not own this goal");
        }
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public GoalType getType() { return type; }
    public Optional<ClimbingGrade> getTargetGrade() { return Optional.ofNullable(targetGrade); }
    public Optional<LocalDate> getTargetDate() { return Optional.ofNullable(targetDate); }
    public boolean isActive() { return active; }
    public Optional<Instant> getAchievedAt() { return Optional.ofNullable(achievedAt); }
    public Optional<String> getNotes() { return Optional.ofNullable(notes); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private void assignGrade(ClimbingGrade grade) {
        if (type.requiresTargetGrade() && grade == null) {
            throw new IllegalArgumentException(
                    "Goal type " + type + " requires a target grade");
        }
        if (!type.requiresTargetGrade() && grade != null) {
            throw new IllegalArgumentException(
                    "Goal type " + type + " does not accept a target grade");
        }
        this.targetGrade = grade;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
