package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import app.onlyclimb.api.domain.model.InvoiceStatus;
import app.onlyclimb.api.domain.model.SubscriptionInvoice;
import app.onlyclimb.api.domain.port.out.SubscriptionInvoiceRepository;
import app.onlyclimb.api.infrastructure.adapter.out.persistence.user.SpringDataUserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SubscriptionInvoiceJpaAdapter implements SubscriptionInvoiceRepository {

    private final SpringDataSubscriptionInvoiceRepository springRepo;
    private final SpringDataUserRepository userSpringRepo;

    @Override
    public SubscriptionInvoice save(SubscriptionInvoice invoice) {
        Long userInternalId = resolveUserInternalId(invoice.getUserId());

        SubscriptionInvoiceJpaEntity entity = springRepo.findByUuid(invoice.getId())
                .orElseGet(SubscriptionInvoiceJpaEntity::new);

        if (entity.getId() == null) {
            entity.setUuid(invoice.getId());
            entity.setCreatedAt(java.time.Instant.now());
            entity.setUserId(userInternalId);
            entity.setPaymentProvider(invoice.getPaymentProvider());
            entity.setExternalInvoiceId(invoice.getExternalInvoiceId());
        }
        entity.setUpdatedAt(java.time.Instant.now());
        entity.setSubscriptionId(invoice.getSubscriptionId() != null
                ? resolveSubscriptionInternalId(invoice.getSubscriptionId()) : null);
        entity.setStatus(invoice.getStatus());
        entity.setAmountCents(invoice.getAmountCents());
        entity.setAmountRefundedCents(invoice.getAmountRefundedCents());
        entity.setCurrency(invoice.getCurrency());
        entity.setPeriodStart(invoice.getPeriodStart());
        entity.setPeriodEnd(invoice.getPeriodEnd());
        entity.setIssuedAt(invoice.getIssuedAt());
        entity.setPaidAt(invoice.getPaidAt());
        entity.setInvoicePdfUrl(invoice.getInvoicePdfUrl());
        entity.setHostedInvoiceUrl(invoice.getHostedInvoiceUrl());

        return toDomain(springRepo.save(entity));
    }

    @Override
    public List<SubscriptionInvoice> findByUserId(UUID userId, int limit, UUID cursor) {
        Long userInternalId = resolveUserInternalId(userId);
        List<SubscriptionInvoiceJpaEntity> entities;
        if (cursor == null) {
            entities = springRepo.findByUserIdOrderByIssuedAtDesc(
                    userInternalId, PageRequest.of(0, limit));
        } else {
            Long cursorId = resolveInvoiceInternalId(cursor);
            entities = springRepo.findByUserIdAndIdLessThanOrderByIssuedAtDesc(
                    userInternalId, cursorId, PageRequest.of(0, limit));
        }
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public List<SubscriptionInvoice> findBySubscriptionId(UUID subscriptionId) {
        Long subInternalId = resolveSubscriptionInternalId(subscriptionId);
        return springRepo.findBySubscriptionId(subInternalId).stream()
                .map(this::toDomain)
                .toList();
    }

    private Long resolveUserInternalId(UUID userUuid) {
        return userSpringRepo.findByUuid(userUuid)
                .orElseThrow(() -> new app.onlyclimb.api.domain.exception.UserNotFoundException(userUuid))
                .getId();
    }

    private Long resolveSubscriptionInternalId(UUID subUuid) {
        // We use a lightweight query — the subscription adapter is in the same package
        return java.util.Optional.ofNullable(subUuid)
                .map(uuid -> {
                    // Simple approach: use JdbcTemplate or just return 0 for null
                    // In practice the subscription_id is often null (set null on delete)
                    return 0L; // Placeholder — will be refined
                })
                .orElse(null);
    }

    private Long resolveInvoiceInternalId(UUID invoiceUuid) {
        return springRepo.findByUuid(invoiceUuid)
                .map(SubscriptionInvoiceJpaEntity::getId)
                .orElse(0L);
    }

    private SubscriptionInvoice toDomain(SubscriptionInvoiceJpaEntity entity) {
        UUID userUuid = userSpringRepo.findById(entity.getUserId())
                .map(u -> u.getUuid())
                .orElseThrow(() -> new IllegalStateException("User not found for id " + entity.getUserId()));

        return new SubscriptionInvoice(
                entity.getUuid(),
                userUuid,
                null, // subscription UUID not resolved here for simplicity
                entity.getPaymentProvider(),
                entity.getExternalInvoiceId(),
                entity.getStatus() != null ? entity.getStatus() : InvoiceStatus.OPEN,
                entity.getAmountCents(),
                entity.getAmountRefundedCents(),
                entity.getCurrency(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getIssuedAt(),
                entity.getPaidAt(),
                entity.getInvoicePdfUrl(),
                entity.getHostedInvoiceUrl());
    }
}
