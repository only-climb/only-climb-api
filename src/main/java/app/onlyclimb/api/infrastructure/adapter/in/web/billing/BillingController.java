package app.onlyclimb.api.infrastructure.adapter.in.web.billing;

import app.onlyclimb.api.domain.port.in.CreateCheckoutSessionCommand;
import app.onlyclimb.api.domain.port.in.CreateCheckoutSessionUseCase;
import app.onlyclimb.api.domain.port.in.CreateCustomerPortalSessionCommand;
import app.onlyclimb.api.domain.port.in.CreateCustomerPortalSessionUseCase;
import app.onlyclimb.api.domain.port.in.GetBillingHistoryUseCase;
import app.onlyclimb.api.domain.port.in.ListInvoicesQuery;
import app.onlyclimb.api.infrastructure.adapter.in.web.auth.CurrentUserService;
import app.onlyclimb.api.infrastructure.adapter.in.web.billing.dto.CheckoutSessionResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.billing.dto.CreateCheckoutSessionRequest;
import app.onlyclimb.api.infrastructure.adapter.in.web.billing.dto.InvoiceResponse;
import app.onlyclimb.api.infrastructure.adapter.in.web.billing.dto.PortalSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Stripe Checkout, Customer Portal, and invoice history")
public class BillingController {

    private final CreateCheckoutSessionUseCase createCheckoutSessionUseCase;
    private final CreateCustomerPortalSessionUseCase createCustomerPortalSessionUseCase;
    private final GetBillingHistoryUseCase getBillingHistoryUseCase;
    private final CurrentUserService currentUserService;

    @PostMapping("/checkout-session")
    @Operation(summary = "Create a Stripe Checkout Session for a new subscription")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            Authentication auth,
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        var user = currentUserService.requireCurrent(auth);
        var command = new CreateCheckoutSessionCommand(
                request.planId(), request.successUrl(), request.cancelUrl());
        var result = createCheckoutSessionUseCase.create(user.getId(), command);
        return ResponseEntity.ok(new CheckoutSessionResponse(result.checkoutUrl()));
    }

    @PostMapping("/customer-portal")
    @Operation(summary = "Create a Stripe Customer Portal session for plan management")
    public ResponseEntity<PortalSessionResponse> createPortalSession(
            Authentication auth,
            @Valid @RequestBody app.onlyclimb.api.infrastructure.adapter.in.web.billing.dto.PortalSessionRequest request) {
        var user = currentUserService.requireCurrent(auth);
        var command = new CreateCustomerPortalSessionCommand(request.returnUrl());
        var result = createCustomerPortalSessionUseCase.create(user.getId(), command);
        return ResponseEntity.ok(new PortalSessionResponse(result.portalUrl()));
    }

    @GetMapping("/invoices")
    @Operation(summary = "List billing history (cursor-paginated)")
    public ResponseEntity<List<InvoiceResponse>> listInvoices(
            Authentication auth,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int limit) {
        var user = currentUserService.requireCurrent(auth);
        var query = new ListInvoicesQuery(user.getId(), cursor, limit);
        var page = getBillingHistoryUseCase.getBillingHistory(query);
        List<InvoiceResponse> items = page.items().stream()
                .map(InvoiceResponse::from)
                .toList();
        // Next cursor is returned as a response header for convenience
        return ResponseEntity.ok()
                .header("X-Next-Cursor", page.nextCursor() != null ? page.nextCursor().toString() : "")
                .body(items);
    }
}
