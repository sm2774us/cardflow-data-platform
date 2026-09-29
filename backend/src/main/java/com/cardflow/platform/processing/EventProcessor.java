package com.cardflow.platform.processing;

import com.cardflow.platform.authorization.AuthorizationEngine;
import com.cardflow.platform.authorization.AuthorizationEngine.Decision;
import com.cardflow.platform.config.AppProperties;
import com.cardflow.platform.domain.CardEvent;
import com.cardflow.platform.domain.Outcome;
import com.cardflow.platform.domain.Records.LiveEvent;
import com.cardflow.platform.repo.OpsRepository;
import com.cardflow.platform.repo.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent consumer core. Everything below happens in ONE database transaction:
 * claim idempotency key -> parse -> DQ gate -> (authorize) -> persist curated row | quarantine.
 * Duplicate deliveries hit the ledger's primary key and are skipped; failures roll back the claim so retries reprocess.
 */
@Service
public class EventProcessor {
    private final ObjectMapper mapper;
    private final DataQualityGate gate;
    private final AuthorizationEngine engine;
    private final TransactionRepository txns;
    private final OpsRepository ops;
    private final JdbcTemplate jdbc;
    private final MeterRegistry metrics;
    private final LiveEventBus live;
    private final AppProperties props;

    public EventProcessor(ObjectMapper mapper, DataQualityGate gate, AuthorizationEngine engine, TransactionRepository txns,
                          OpsRepository ops, JdbcTemplate jdbc, MeterRegistry metrics, LiveEventBus live, AppProperties props) {
        this.mapper = mapper; this.gate = gate; this.engine = engine; this.txns = txns; this.ops = ops;
        this.jdbc = jdbc; this.metrics = metrics; this.live = live; this.props = props;
    }

    @Transactional
    public Outcome process(String messageId, String raw) {
        Timer.Sample sample = Timer.start(metrics);
        JsonNode tree = null;
        try { tree = mapper.readTree(raw); } catch (JsonProcessingException ignored) { /* quarantined below */ }
        String id = messageId != null ? messageId : (tree != null ? tree.path("eventId").asText(null) : null);
        if (id == null || id.isBlank()) id = "unknown-" + shortHash(raw);

        int claimed = jdbc.update("INSERT INTO processed_event(event_id, outcome) VALUES (?, 'PENDING') ON CONFLICT DO NOTHING", id);
        if (claimed == 0) {
            metrics.counter("cardflow.events.duplicates").increment();
            live.publish(new LiveEvent(id, Outcome.DUPLICATE, null, null, null, "dropped by idempotent consumer", Instant.now()));
            return Outcome.DUPLICATE;
        }

        if (props.chaos().enabled() && tree != null && "throw".equals(tree.path("__chaos").asText())) {
            throw new IllegalStateException("Simulated downstream failure for event " + id);
        }

        CardEvent ev = null;
        if (tree != null) {
            try { ev = mapper.treeToValue(tree, CardEvent.class); } catch (JsonProcessingException ignored) { /* quarantined below */ }
        }
        if (ev == null) return quarantine(id, raw, List.of("SCHEMA_PARSE"), null, sample);
        List<String> failed = gate.evaluate(ev);
        if (!failed.isEmpty()) return quarantine(id, raw, failed, ev, sample);

        ev = new CardEvent(id, ev.schemaVersion(), ev.eventType(), ev.cardToken(), ev.merchantName(), ev.mcc(),
                ev.amount(), ev.currency(), ev.country(), ev.occurredAt());
        Decision d;
        String decision;
        switch (ev.eventType()) {
            case "AUTHORIZATION" -> {
                d = engine.decide(ev.cardToken(), ev.mcc(), ev.amount(), ev.country());
                decision = d.approved() ? "APPROVED" : "DECLINED";
            }
            case "CLEARING" -> { d = new Decision(true, List.of(), 0.0, false); decision = "SETTLED"; }
            default -> { d = new Decision(true, List.of(), 0.0, false); decision = "REVERSED"; }
        }
        txns.insert(ev, d, decision);
        Outcome outcome = d.approved() ? Outcome.APPROVED : Outcome.DECLINED;
        finish(id, outcome, sample);
        live.publish(new LiveEvent(id, outcome, ev.merchantName(), ev.amount(), ev.currency(),
                d.reasons().isEmpty() ? "risk=" + d.riskScore() : String.join(",", d.reasons()), Instant.now()));
        return outcome;
    }

    private Outcome quarantine(String id, String raw, List<String> failed, CardEvent ev, Timer.Sample sample) {
        ops.saveQuarantine(id, raw, String.join(",", failed));
        finish(id, Outcome.QUARANTINED, sample);
        live.publish(new LiveEvent(id, Outcome.QUARANTINED, ev == null ? null : ev.merchantName(),
                ev == null ? null : ev.amount(), ev == null ? null : ev.currency(), String.join(",", failed), Instant.now()));
        return Outcome.QUARANTINED;
    }

    private void finish(String id, Outcome outcome, Timer.Sample sample) {
        jdbc.update("UPDATE processed_event SET outcome = ? WHERE event_id = ?", outcome.name(), id);
        metrics.counter("cardflow.events.processed", "outcome", outcome.name()).increment();
        sample.stop(metrics.timer("cardflow.processing.latency"));
    }

    private static String shortHash(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
