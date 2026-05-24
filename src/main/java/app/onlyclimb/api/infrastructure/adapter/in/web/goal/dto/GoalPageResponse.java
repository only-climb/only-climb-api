package app.onlyclimb.api.infrastructure.adapter.in.web.goal.dto;

import java.util.List;

public record GoalPageResponse(List<GoalResponse> data, String nextCursor) {}
