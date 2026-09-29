package com.cardflow.platform.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionView(
        String eventId, String eventType, String cardToken, String merchantName, String mcc,
        BigDecimal amount, String currency, String country, Instant occurredAt,
        String decision, String declineReason, BigDecimal riskScore, Instant createdAt) {}
