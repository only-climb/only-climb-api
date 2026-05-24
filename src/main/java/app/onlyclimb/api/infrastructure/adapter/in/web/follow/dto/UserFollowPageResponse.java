package app.onlyclimb.api.infrastructure.adapter.in.web.follow.dto;

import java.util.List;

public record UserFollowPageResponse(List<UserFollowEntryResponse> data, String nextCursor) {}
