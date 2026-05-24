package app.onlyclimb.api.infrastructure.adapter.in.web.trainingplan.dto;

import java.util.List;

public record TrainingPlanPageResponse(
        List<TrainingPlanResponse> data,
        String nextCursor) {
}
