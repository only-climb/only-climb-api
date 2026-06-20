package app.onlyclimb.api.domain.port.in;

import java.util.UUID;

public record ListInvoicesQuery(
        UUID userId,
        UUID cursor,
        int limit) {
    public ListInvoicesQuery {
        if (limit <= 0) limit = 20;
        if (limit > 100) limit = 100;
    }
}
