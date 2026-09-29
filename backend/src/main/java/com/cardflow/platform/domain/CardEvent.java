package com.cardflow.platform.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** Inbound card event (schema v1). Fields are nullable on purpose: validation is the DQ gate's job, not the parser's. */
public record CardEvent(
        String eventId,
        Integer schemaVersion,
        String eventType,
        String cardToken,
        String merchantName,
        String mcc,
        BigDecimal amount,
        String currency,
        String country,
        Instant occurredAt) {}
