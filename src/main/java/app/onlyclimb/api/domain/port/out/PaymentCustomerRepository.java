package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.PaymentCustomer;

import java.util.Optional;
import java.util.UUID;

public interface PaymentCustomerRepository {
    PaymentCustomer save(PaymentCustomer customer);
    Optional<PaymentCustomer> findByUserIdAndProvider(UUID userId, String provider);
    Optional<PaymentCustomer> findByProviderAndExternalId(String provider, String externalCustomerId);
}
