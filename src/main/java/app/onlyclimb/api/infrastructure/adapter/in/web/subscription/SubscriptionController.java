package app.onlyclimb.api.infrastructure.adapter.in.web.subscription;

import app.onlyclimb.api.domain.port.in.GetCurrentSubscriptionUseCase;
import app.onlyclimb.api.domain.port.in.GetSubscriptionTiersUseCase;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.CurrentUserService;
import app.onlyclimb.api.infrastructure.adapter.in.web.subscription.dto.CurrentSubscriptionResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.subscription.dto.TierResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Tier catalog and current subscription")
public class SubscriptionController {

    private final GetSubscriptionTiersUseCase getSubscriptionTiersUseCase;
    private final GetCurrentSubscriptionUseCase getCurrentSubscriptionUseCase;
    private final CurrentUserService currentUserService;

    @GetMapping("/tiers")
    @Operation(summary = "List available subscription tiers with their plans")
    public ResponseEntity<List<TierResponse>> listTiers() {
        var tiers = getSubscriptionTiersUseCase.getTiers();
        return ResponseEntity.ok(TierResponse.fromList(tiers));
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user's current subscription")
    public ResponseEntity<CurrentSubscriptionResponse> getMySubscription(Authentication auth) {
        var user = currentUserService.requireCurrent(auth);
        var sub = getCurrentSubscriptionUseCase.getCurrent(user.getId());
        return ResponseEntity.ok(CurrentSubscriptionResponse.from(sub));
    }
}
