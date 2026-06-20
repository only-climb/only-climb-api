package app.onlyclimb.api.domain.exception;

import java.util.UUID;

public class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException(UUID userId) {
        super("No active subscription for user " + userId);
    }

    public SubscriptionNotFoundException(String message) {
        super(message);
    }
}
