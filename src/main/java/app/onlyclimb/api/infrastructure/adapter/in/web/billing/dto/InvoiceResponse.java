package app.onlyclimb.api.infrastructure.adapter.in.web.billing.dto;

import app.onlyclimb.api.domain.port.in.GetBillingHistoryUseCase.InvoiceSummary;

import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID invoiceId,
        String status,
        int amountCents,
        int amountRefundedCents,
        String currency,
        Instant periodStart,
        Instant periodEnd,
        Instant issuedAt,
        Instant paidAt,
        String hostedInvoiceUrl,
        String invoicePdfUrl
) {
    public static InvoiceResponse from(InvoiceSummary summary) {
        return new InvoiceResponse(
                summary.invoiceId(),
                summary.status().name(),
                summary.amountCents(),
                summary.amountRefundedCents(),
                summary.currency(),
                summary.periodStart(),
                summary.periodEnd(),
                summary.issuedAt(),
                summary.paidAt(),
                summary.hostedInvoiceUrl(),
                summary.invoicePdfUrl());
    }
}
