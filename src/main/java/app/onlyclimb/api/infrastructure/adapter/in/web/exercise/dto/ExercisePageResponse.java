package app.onlyclimb.api.infrastructure.adapter.in.web.exercise.dto;

import java.util.List;

public record ExercisePageResponse(List<ExerciseResponse> data, String nextCursor) {
}
