package app.onlyclimb.api.domain.model;

/**
 * Mirrors the PostgreSQL {@code invoice_status} enum.
 */
public enum InvoiceStatus {
    DRAFT,
    OPEN,
    PAID,
    UNCOLLECTIBLE,
    VOID,
    REFUNDED
}
