package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public interface CreateCheckoutSessionUseCase {
    /** Returns the Stripe Checkout URL the client should redirect to. */
    record CheckoutSessionResponse(String checkoutUrl) {}

    CheckoutSessionResponse create(UUID userId, CreateCheckoutSessionCommand command);
}
