package com.cardflow.platform.domain;

import java.time.Instant;
import java.util.List;

/** Small API/read-model records grouped in one file to keep the domain package navigable. */
public final class Records {
    private Records() {}

    public record PageResult<T>(List<T> items, long total, int page, int size) {}
    public record QuarantineItem(long id, String eventId, String rawPayload, String failedRules, Instant createdAt, boolean replayed) {}
    public record DeadLetterItem(long id, String eventId, String rawPayload, String error, Instant createdAt, boolean replayed) {}
    public record ThroughputPoint(Instant minute, long total, long declined) {}
    public record PipelineStats(
            long outboxPending, long transactions, long approved, long declined, long quarantined,
            long deadLetters, long duplicatesDropped, long lakeBatches, long lakeRecords, long unexported,
            double dqPassRate, List<ThroughputPoint> throughput) {}
    public record LiveEvent(String eventId, Outcome outcome, String merchant, java.math.BigDecimal amount,
                            String currency, String detail, Instant at) {}
    public record DqRuleInfo(String code, String description) {}
}
