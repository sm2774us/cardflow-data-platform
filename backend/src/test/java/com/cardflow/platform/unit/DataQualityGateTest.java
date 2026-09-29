package com.cardflow.platform.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.cardflow.platform.domain.CardEvent;
import com.cardflow.platform.processing.DataQualityGate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class DataQualityGateTest {
    private final DataQualityGate gate = new DataQualityGate();

    private static CardEvent valid() {
        return new CardEvent("e-1", 1, "AUTHORIZATION", "tok_ABCDEFGH1234", "Cafe Aroma", "5814",
                new BigDecimal("12.50"), "USD", "US", Instant.now().minusSeconds(5));
    }

    @Test void validEventPassesAllRules() { assertThat(gate.evaluate(valid())).isEmpty(); }

    @Test void negativeAmountFailsRange() {
        var e = valid();
        var bad = new CardEvent(e.eventId(), 1, e.eventType(), e.cardToken(), e.merchantName(), e.mcc(), new BigDecimal("-1"), "USD", "US", e.occurredAt());
        assertThat(gate.evaluate(bad)).containsExactly("AMOUNT_RANGE");
    }

    @Test void unknownCurrencyFails() {
        var e = valid();
        var bad = new CardEvent(e.eventId(), 1, e.eventType(), e.cardToken(), e.merchantName(), e.mcc(), e.amount(), "XXA", "US", e.occurredAt());
        assertThat(gate.evaluate(bad)).containsExactly("CURRENCY_ISO4217");
    }

    @Test void panInTokenIsBlockedByPciGuard() {
        var e = valid();
        var bad = new CardEvent(e.eventId(), 1, e.eventType(), "4111111111111111", e.merchantName(), e.mcc(), e.amount(), "USD", "US", e.occurredAt());
        assertThat(gate.evaluate(bad)).contains("NO_PAN_LEAK", "TOKENIZED_CARD");
    }

    @Test void futureTimestampFails() {
        var e = valid();
        var bad = new CardEvent(e.eventId(), 1, e.eventType(), e.cardToken(), e.merchantName(), e.mcc(), e.amount(), "USD", "US", Instant.now().plus(2, ChronoUnit.HOURS));
        assertThat(gate.evaluate(bad)).containsExactly("TIMESTAMP_SANE");
    }

    @Test void missingFieldsNeverThrow() {
        assertThat(gate.evaluate(new CardEvent("e-2", null, null, null, null, null, null, null, null, null)))
                .contains("REQUIRED_FIELDS", "SCHEMA_VERSION");
    }
}
