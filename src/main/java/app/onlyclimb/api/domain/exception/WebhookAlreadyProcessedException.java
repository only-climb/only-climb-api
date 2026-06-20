package app.onlyclimb.api.domain.exception;

public class WebhookAlreadyProcessedException extends RuntimeException {
    public WebhookAlreadyProcessedException(String provider, String externalEventId) {
        super(String.format("Webhook event already processed: %s/%s", provider, externalEventId));
    }
}
