package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.Goal;
import app.onlyclimb.api.domain.port.in.ListGoalsQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository {

    Goal save(Goal goal);

    Optional<Goal> findById(UUID id);

    Optional<Goal> findActiveByOwner(UUID ownerId);

    void deleteById(UUID id);

    Page<Goal> search(ListGoalsQuery query);

    record Page<T>(List<T> items, String nextCursor) {}
}
