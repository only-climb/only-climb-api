package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import app.onlyclimb.api.domain.model.PaymentWebhookEvent;
import app.onlyclimb.api.domain.port.out.PaymentWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PaymentWebhookEventJpaAdapter implements PaymentWebhookEventRepository {

    private final SpringDataPaymentWebhookEventRepository springRepo;

    @Override
    public PaymentWebhookEvent save(PaymentWebhookEvent event) {
        PaymentWebhookEventJpaEntity entity = springRepo.findByUuid(event.getId())
                .orElseGet(PaymentWebhookEventJpaEntity::new);

        if (entity.getId() == null) {
            entity.setUuid(event.getId());
            entity.setReceivedAt(event.getReceivedAt());
            entity.setPaymentProvider(event.getPaymentProvider());
            entity.setExternalEventId(event.getExternalEventId());
            entity.setEventType(event.getEventType());
            entity.setPayload(event.getPayload());
        }
        entity.setProcessedAt(event.getProcessedAt());
        entity.setProcessingError(event.getProcessingError());

        return toDomain(springRepo.save(entity));
    }

    @Override
    public boolean existsByProviderAndExternalEventId(String provider, String externalEventId) {
        return springRepo.existsByPaymentProviderAndExternalEventId(provider, externalEventId);
    }

    private PaymentWebhookEvent toDomain(PaymentWebhookEventJpaEntity entity) {
        return new PaymentWebhookEvent(
                entity.getUuid(),
                entity.getReceivedAt(),
                entity.getPaymentProvider(),
                entity.getExternalEventId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getProcessedAt(),
                entity.getProcessingError());
    }
}
