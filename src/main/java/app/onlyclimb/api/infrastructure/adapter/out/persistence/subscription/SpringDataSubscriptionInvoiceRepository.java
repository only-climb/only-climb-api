package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoiceJpaEntity, Long> {

    Optional<SubscriptionInvoiceJpaEntity> findByUuid(UUID uuid);

    java.util.List<SubscriptionInvoiceJpaEntity> findByUserIdOrderByIssuedAtDesc(Long userId, Pageable pageable);

    java.util.List<SubscriptionInvoiceJpaEntity> findByUserIdAndIdLessThanOrderByIssuedAtDesc(
            Long userId, Long cursor, Pageable pageable);

    java.util.List<SubscriptionInvoiceJpaEntity> findBySubscriptionId(Long subscriptionId);
}
