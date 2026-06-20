package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEventJpaEntity, Long> {

    java.util.Optional<PaymentWebhookEventJpaEntity> findByUuid(UUID uuid);

    boolean existsByPaymentProviderAndExternalEventId(String paymentProvider, String externalEventId);
}
