package app.onlyclimb.api.domain.port.in;

import app.onlyclimb.api.domain.model.InvoiceStatus;

import java.time.Instant;
import java.util.UUID;

public interface GetBillingHistoryUseCase {
    record InvoiceSummary(
            UUID invoiceId,
            InvoiceStatus status,
            int amountCents,
            int amountRefundedCents,
            String currency,
            Instant periodStart,
            Instant periodEnd,
            Instant issuedAt,
            Instant paidAt,
            String hostedInvoiceUrl,
            String invoicePdfUrl
    ) {}

    record InvoicePage(java.util.List<InvoiceSummary> items, UUID nextCursor) {}

    InvoicePage getBillingHistory(ListInvoicesQuery query);
}
