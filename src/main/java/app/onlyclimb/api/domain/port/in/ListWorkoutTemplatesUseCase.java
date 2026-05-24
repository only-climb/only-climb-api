package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.WorkoutTemplate;
import app.onlyclimb.api.domain.port.out.WorkoutTemplateRepository.Page;

public interface ListWorkoutTemplatesUseCase {
    Page<WorkoutTemplate> list(ListWorkoutTemplatesQuery query);
}
