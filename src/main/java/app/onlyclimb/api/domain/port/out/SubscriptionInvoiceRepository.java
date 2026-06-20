package app.onlyclimb.api.domain.port.out;

import app.onlyclimb.api.domain.model.SubscriptionInvoice;

import java.util.List;
import java.util.UUID;

public interface SubscriptionInvoiceRepository {
    SubscriptionInvoice save(SubscriptionInvoice invoice);
    List<SubscriptionInvoice> findByUserId(UUID userId, int limit, UUID cursor);
    List<SubscriptionInvoice> findBySubscriptionId(UUID subscriptionId);
}
