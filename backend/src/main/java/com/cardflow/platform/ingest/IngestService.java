package com.cardflow.platform.ingest;

import com.cardflow.platform.domain.Outcome;
import com.cardflow.platform.domain.Records.LiveEvent;
import com.cardflow.platform.processing.LiveEventBus;
import com.cardflow.platform.repo.OpsRepository;
import com.cardflow.platform.support.ApiExceptionHandler.BadRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Ingress: accepts any JSON with an {@code eventId} into the transactional outbox. Semantic validation is deliberately
 * deferred to the DQ gate so bad data is quarantined (auditable, replayable) rather than rejected and lost.
 */
@Service
public class IngestService {
    public record IngestResult(String eventId, boolean duplicate) {}

    private final ObjectMapper mapper;
    private final OpsRepository ops;
    private final MeterRegistry metrics;
    private final LiveEventBus live;

    public IngestService(ObjectMapper mapper, OpsRepository ops, MeterRegistry metrics, LiveEventBus live) {
        this.mapper = mapper; this.ops = ops; this.metrics = metrics; this.live = live;
    }

    public IngestResult accept(String raw) {
        JsonNode node;
        try { node = mapper.readTree(raw); } catch (JsonProcessingException e) { throw new BadRequest("Body must be valid JSON"); }
        String id = node == null ? null : node.path("eventId").asText(null);
        if (id == null || id.isBlank() || id.length() > 64) throw new BadRequest("eventId is required (max 64 chars)");
        boolean isNew = ops.insertOutbox(id, raw);
        metrics.counter("cardflow.events.ingested", "result", isNew ? "new" : "duplicate").increment();
        if (!isNew) {
            metrics.counter("cardflow.events.duplicates").increment();
            live.publish(new LiveEvent(id, Outcome.DUPLICATE, null, null, null, "dropped at ingress (outbox unique key)", Instant.now()));
        }
        return new IngestResult(id, !isNew);
    }
}
