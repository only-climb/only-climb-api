package app.onlyclimb.api.domain.exception;

public class QuotaExceededException extends RuntimeException {
    private final String resource;
    private final int limit;
    private final int current;

    public QuotaExceededException(String resource, int limit, int current) {
        super(String.format("Quota exceeded for %s: limit %d, current %d", resource, limit, current));
        this.resource = resource;
        this.limit = limit;
        this.current = current;
    }

    public String getResource() { return resource; }
    public int getLimit() { return limit; }
    public int getCurrent() { return current; }
}
