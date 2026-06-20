package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPaymentCustomerRepository extends JpaRepository<PaymentCustomerJpaEntity, Long> {

    Optional<PaymentCustomerJpaEntity> findByUuid(UUID uuid);

    Optional<PaymentCustomerJpaEntity> findByUserIdAndPaymentProvider(Long userId, String paymentProvider);

    Optional<PaymentCustomerJpaEntity> findByPaymentProviderAndExternalCustomerId(
            String paymentProvider, String externalCustomerId);
}
