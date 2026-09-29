package com.cardflow.platform.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.cardflow.platform.config.AppProperties;
import com.cardflow.platform.ingest.SignatureVerifier;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SignatureVerifierTest {
    private final SignatureVerifier verifier = new SignatureVerifier(
            new AppProperties(new AppProperties.Webhook("s3cret"), null, null, null, null, null));

    @Test void acceptsValidSignature() {
        byte[] body = "{\"eventId\":\"a\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(verifier.verify(body, verifier.sign(body))).isTrue();
    }

    @Test void rejectsTamperedBodyMissingAndMalformedHeaders() {
        byte[] body = "{\"eventId\":\"a\"}".getBytes(StandardCharsets.UTF_8);
        String sig = verifier.sign(body);
        assertThat(verifier.verify("{\"eventId\":\"b\"}".getBytes(StandardCharsets.UTF_8), sig)).isFalse();
        assertThat(verifier.verify(body, null)).isFalse();
        assertThat(verifier.verify(body, "sha256=deadbeef")).isFalse();
    }
}
