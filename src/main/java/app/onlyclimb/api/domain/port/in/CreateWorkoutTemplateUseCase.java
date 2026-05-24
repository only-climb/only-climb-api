package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.WorkoutTemplate;

public interface CreateWorkoutTemplateUseCase {
    WorkoutTemplate create(CreateWorkoutTemplateCommand command);
}
