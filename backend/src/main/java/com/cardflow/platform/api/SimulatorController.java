package com.cardflow.platform.api;

import com.cardflow.platform.config.AppProperties;
import com.cardflow.platform.ingest.IngestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.web.bind.annotation.*;

/** Demo traffic generator: valid, duplicate, invalid and poison events, injected through the same ingest path as the webhook. */
@RestController
@RequestMapping("/api/v1/simulator")
public class SimulatorController {
    public record BurstRequest(@Min(1) @Max(2000) int count, @Min(0) @Max(100) int duplicatePct,
                               @Min(0) @Max(100) int invalidPct, @Min(0) @Max(20) int poisonCount) {}

    private static final String[][] MERCHANTS = {
            {"Cafe Aroma", "5814"}, {"Supermercado Sol", "5411"}, {"Uber Trip", "4121"}, {"Airline Tickets", "3000"},
            {"Electronics Hub", "5732"}, {"Online Casino", "7995"}, {"FX Wire Service", "4829"}, {"Fuel Station 24", "5541"}};
    private static final String[] COUNTRIES = {"US", "US", "US", "BR", "MX", "AR", "GB", "CL", "KP"};

    private final IngestService ingest;
    private final ObjectMapper mapper;
    private final AppProperties props;

    public SimulatorController(IngestService ingest, ObjectMapper mapper, AppProperties props) {
        this.ingest = ingest; this.mapper = mapper; this.props = props;
    }

    @PostMapping("/burst")
    public Map<String, Object> burst(@Valid @RequestBody BurstRequest req) throws JsonProcessingException {
        var rnd = ThreadLocalRandom.current();
        List<String> sent = new ArrayList<>();
        int valid = 0, invalid = 0, dupes = 0, poison = 0;
        for (int i = 0; i < req.count(); i++) {
            if (!sent.isEmpty() && rnd.nextInt(100) < req.duplicatePct()) {
                ingest.accept(sent.get(rnd.nextInt(sent.size())));
                dupes++;
                continue;
            }
            boolean bad = rnd.nextInt(100) < req.invalidPct();
            String raw = mapper.writeValueAsString(bad ? corrupt(event(rnd)) : event(rnd));
            ingest.accept(raw);
            sent.add(raw);
            if (bad) invalid++; else valid++;
        }
        if (props.chaos().enabled()) {
            for (int i = 0; i < req.poisonCount(); i++) {
                Map<String, Object> e = event(rnd);
                e.put("__chaos", "throw");
                ingest.accept(mapper.writeValueAsString(e));
                poison++;
            }
        }
        return Map.of("valid", valid, "invalid", invalid, "duplicates", dupes, "poison", poison);
    }

    private Map<String, Object> event(ThreadLocalRandom rnd) {
        String[] m = MERCHANTS[rnd.nextInt(MERCHANTS.length)];
        double amt = Math.min(9000, Math.exp(rnd.nextDouble(2.0, 7.2)));
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("eventId", UUID.randomUUID().toString());
        e.put("schemaVersion", 1);
        e.put("eventType", rnd.nextInt(10) < 8 ? "AUTHORIZATION" : (rnd.nextBoolean() ? "CLEARING" : "REVERSAL"));
        e.put("cardToken", "tok_" + String.format("%012d", rnd.nextInt(30)));
        e.put("merchantName", m[0]);
        e.put("mcc", m[1]);
        e.put("amount", BigDecimal.valueOf(Math.round(amt * 100) / 100.0));
        e.put("currency", rnd.nextInt(10) < 7 ? "USD" : "BRL");
        e.put("country", COUNTRIES[rnd.nextInt(COUNTRIES.length)]);
        e.put("occurredAt", Instant.now().minus(rnd.nextInt(30), ChronoUnit.SECONDS).toString());
        return e;
    }

    private Map<String, Object> corrupt(Map<String, Object> e) {
        switch (ThreadLocalRandom.current().nextInt(5)) {
            case 0 -> e.put("amount", -12.5);
            case 1 -> e.put("currency", "XXA");
            case 2 -> e.put("mcc", "12");
            case 3 -> e.put("cardToken", "4111111111111111");
            default -> e.put("occurredAt", Instant.now().plus(2, ChronoUnit.HOURS).toString());
        }
        return e;
    }
}
