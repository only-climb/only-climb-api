package app.onlyclimb.api.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Historical billing record. Driven by Stripe webhooks.
 */
public class SubscriptionInvoice {

    private final UUID id;
    private final UUID userId;
    private final UUID subscriptionId;
    private final String paymentProvider;
    private final String externalInvoiceId;
    private final InvoiceStatus status;
    private final int amountCents;
    private final int amountRefundedCents;
    private final String currency;
    private final Instant periodStart;
    private final Instant periodEnd;
    private final Instant issuedAt;
    private final Instant paidAt;
    private final String invoicePdfUrl;
    private final String hostedInvoiceUrl;

    public SubscriptionInvoice(UUID id, UUID userId, UUID subscriptionId,
                               String paymentProvider, String externalInvoiceId,
                               InvoiceStatus status, int amountCents, int amountRefundedCents,
                               String currency, Instant periodStart, Instant periodEnd,
                               Instant issuedAt, Instant paidAt,
                               String invoicePdfUrl, String hostedInvoiceUrl) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.subscriptionId = subscriptionId;
        this.paymentProvider = Objects.requireNonNull(paymentProvider, "paymentProvider is required");
        this.externalInvoiceId = Objects.requireNonNull(externalInvoiceId, "externalInvoiceId is required");
        this.status = Objects.requireNonNull(status, "status is required");
        if (amountCents < 0) throw new IllegalArgumentException("amountCents must be >= 0");
        if (amountRefundedCents < 0) throw new IllegalArgumentException("amountRefundedCents must be >= 0");
        if (amountRefundedCents > amountCents) throw new IllegalArgumentException("refund exceeds amount");
        this.amountCents = amountCents;
        this.amountRefundedCents = amountRefundedCents;
        this.currency = Objects.requireNonNull(currency, "currency is required");
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.issuedAt = issuedAt;
        this.paidAt = paidAt;
        this.invoicePdfUrl = invoicePdfUrl;
        this.hostedInvoiceUrl = hostedInvoiceUrl;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public String getPaymentProvider() { return paymentProvider; }
    public String getExternalInvoiceId() { return externalInvoiceId; }
    public InvoiceStatus getStatus() { return status; }
    public int getAmountCents() { return amountCents; }
    public int getAmountRefundedCents() { return amountRefundedCents; }
    public String getCurrency() { return currency; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getPaidAt() { return paidAt; }
    public String getInvoicePdfUrl() { return invoicePdfUrl; }
    public String getHostedInvoiceUrl() { return hostedInvoiceUrl; }
}
