package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.WorkoutTemplate;

public interface UpdateWorkoutTemplateUseCase {
    WorkoutTemplate update(UpdateWorkoutTemplateCommand command);
}
