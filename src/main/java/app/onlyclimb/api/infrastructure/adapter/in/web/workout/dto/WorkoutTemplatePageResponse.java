package app.onlyclimb.api.infrastructure.adapter.in.web.workout.dto;

import java.util.List;

public record WorkoutTemplatePageResponse(List<WorkoutTemplateResponse> data, String nextCursor) {
}
