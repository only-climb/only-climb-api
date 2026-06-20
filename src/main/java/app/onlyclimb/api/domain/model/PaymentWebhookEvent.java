package app.onlyclimb.api.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Inbound webhook event from a payment provider (Stripe).
 * The UNIQUE constraint on (provider, externalEventId) gives at-most-once
 * processing semantics at the database level.
 */
public class PaymentWebhookEvent {

    private final UUID id;
    private final Instant receivedAt;
    private final String paymentProvider;
    private final String externalEventId;
    private final String eventType;
    private final String payload; // raw JSON
    private Instant processedAt;
    private String processingError;

    public PaymentWebhookEvent(UUID id, Instant receivedAt, String paymentProvider,
                               String externalEventId, String eventType, String payload,
                               Instant processedAt, String processingError) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt is required");
        this.paymentProvider = Objects.requireNonNull(paymentProvider, "paymentProvider is required");
        this.externalEventId = Objects.requireNonNull(externalEventId, "externalEventId is required");
        this.eventType = Objects.requireNonNull(eventType, "eventType is required");
        this.payload = Objects.requireNonNull(payload, "payload is required");
        this.processedAt = processedAt;
        this.processingError = processingError;
    }

    /** Factory: event received but not yet processed. */
    public static PaymentWebhookEvent received(UUID id, String provider, String externalEventId,
                                              String eventType, String payload) {
        return new PaymentWebhookEvent(id, Instant.now(), provider, externalEventId,
                eventType, payload, null, null);
    }

    public void markProcessed() {
        this.processedAt = Instant.now();
        this.processingError = null;
    }

    public void markFailed(String error) {
        this.processedAt = Instant.now();
        this.processingError = Objects.requireNonNull(error);
    }

    public boolean isProcessed() {
        return processedAt != null;
    }

    public UUID getId() { return id; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getPaymentProvider() { return paymentProvider; }
    public String getExternalEventId() { return externalEventId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getProcessedAt() { return processedAt; }
    public String getProcessingError() { return processingError; }
}
