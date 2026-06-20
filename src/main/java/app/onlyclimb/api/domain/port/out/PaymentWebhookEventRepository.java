package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.PaymentWebhookEvent;

public interface PaymentWebhookEventRepository {
    PaymentWebhookEvent save(PaymentWebhookEvent event);
    boolean existsByProviderAndExternalEventId(String provider, String externalEventId);
}
