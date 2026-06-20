package app.onlyclimb.api.infrastructure.adapter.out.persistence.subscription;

import app.onlyclimb.api.domain.model.PaymentCustomer;
import app.onlyclimb.api.domain.port.out.PaymentCustomerRepository;
import app.onlyclimb.api.infrastructure.adapter.out.persistence.user.SpringDataUserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PaymentCustomerJpaAdapter implements PaymentCustomerRepository {

    private final SpringDataPaymentCustomerRepository springRepo;
    private final SpringDataUserRepository userSpringRepo;

    @Override
    public PaymentCustomer save(PaymentCustomer customer) {
        Long userInternalId = resolveUserInternalId(customer.getUserId());

        PaymentCustomerJpaEntity entity = springRepo
                .findByUserIdAndPaymentProvider(userInternalId, customer.getPaymentProvider())
                .orElseGet(PaymentCustomerJpaEntity::new);

        if (entity.getId() == null) {
            entity.setUuid(customer.getId());
            entity.setCreatedAt(java.time.Instant.now());
            entity.setUserId(userInternalId);
            entity.setPaymentProvider(customer.getPaymentProvider());
        }
        entity.setUpdatedAt(java.time.Instant.now());
        entity.setExternalCustomerId(customer.getExternalCustomerId());

        return toDomain(springRepo.save(entity));
    }

    @Override
    public Optional<PaymentCustomer> findByUserIdAndProvider(UUID userId, String provider) {
        return resolveUserInternalIdOptional(userId)
                .flatMap(internalId -> springRepo.findByUserIdAndPaymentProvider(internalId, provider))
                .map(this::toDomain);
    }

    @Override
    public Optional<PaymentCustomer> findByProviderAndExternalId(String provider, String externalCustomerId) {
        return springRepo.findByPaymentProviderAndExternalCustomerId(provider, externalCustomerId)
                .map(this::toDomain);
    }

    private Long resolveUserInternalId(UUID userUuid) {
        return userSpringRepo.findByUuid(userUuid)
                .orElseThrow(() -> new app.onlyclimb.api.domain.exception.UserNotFoundException(userUuid))
                .getId();
    }

    private Optional<Long> resolveUserInternalIdOptional(UUID userUuid) {
        return userSpringRepo.findByUuid(userUuid)
                .map(user -> user.getId());
    }

    private PaymentCustomer toDomain(PaymentCustomerJpaEntity entity) {
        UUID userUuid = userSpringRepo.findById(entity.getUserId())
                .map(u -> u.getUuid())
                .orElseThrow(() -> new IllegalStateException("User not found for id " + entity.getUserId()));

        return new PaymentCustomer(
                entity.getUuid(),
                userUuid,
                entity.getPaymentProvider(),
                entity.getExternalCustomerId());
    }
}
