package com.cardflow.platform.ingest;

import com.cardflow.platform.support.ApiExceptionHandler.Unauthorized;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {
    private final SignatureVerifier verifier;
    private final IngestService ingest;

    public WebhookController(SignatureVerifier verifier, IngestService ingest) {
        this.verifier = verifier; this.ingest = ingest;
    }

    /** Idempotent: re-delivering the same eventId returns 202 with duplicate=true and has no further effect. */
    @PostMapping(value = "/card-events", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> receive(@RequestBody byte[] body,
            @RequestHeader(value = "X-Signature", required = false) String signature) {
        if (!verifier.verify(body, signature)) throw new Unauthorized("Invalid or missing X-Signature");
        var result = ingest.accept(new String(body, StandardCharsets.UTF_8));
        return ResponseEntity.accepted().body(Map.of("eventId", result.eventId(), "duplicate", result.duplicate()));
    }
}
