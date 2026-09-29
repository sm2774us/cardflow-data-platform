package com.cardflow.platform.api;

import com.cardflow.platform.domain.Records.*;
import com.cardflow.platform.processing.DataQualityGate;
import com.cardflow.platform.repo.OpsRepository;
import com.cardflow.platform.support.ApiExceptionHandler.NotFound;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PipelineController {
    private final OpsRepository ops;
    private final DataQualityGate gate;
    private final MeterRegistry metrics;

    public PipelineController(OpsRepository ops, DataQualityGate gate, MeterRegistry metrics) {
        this.ops = ops; this.gate = gate; this.metrics = metrics;
    }

    @GetMapping("/pipeline/stats")
    public PipelineStats stats() {
        return ops.stats((long) metrics.counter("cardflow.events.duplicates").count());
    }

    @GetMapping("/dq/rules")
    public List<DqRuleInfo> rules() { return gate.describe(); }

    @GetMapping("/dq/quarantine")
    public List<QuarantineItem> quarantine(@RequestParam(defaultValue = "50") int limit) { return ops.quarantine(clamp(limit)); }

    @PostMapping("/dq/quarantine/{id}/replay")
    public Map<String, Object> replayQuarantine(@PathVariable long id, @RequestBody(required = false) Map<String, String> body) {
        return replay("dq_quarantine", id, body);
    }

    @GetMapping("/dlq")
    public List<DeadLetterItem> deadLetters(@RequestParam(defaultValue = "50") int limit) { return ops.deadLetters(clamp(limit)); }

    @PostMapping("/dlq/{id}/replay")
    public Map<String, Object> replayDlq(@PathVariable long id, @RequestBody(required = false) Map<String, String> body) {
        return replay("dead_letter", id, body);
    }

    private Map<String, Object> replay(String table, long id, Map<String, String> body) {
        boolean ok = ops.replay(table, id, body == null ? null : body.get("payload"));
        if (!ok) throw new NotFound("Item " + id + " not found or already replayed");
        return Map.of("id", id, "replayed", true);
    }

    private static int clamp(int n) { return Math.min(Math.max(n, 1), 200); }
}
