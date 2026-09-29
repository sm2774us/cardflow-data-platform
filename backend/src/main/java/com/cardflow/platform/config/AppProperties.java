package com.cardflow.platform.config;

import java.math.BigDecimal;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cardflow")
public record AppProperties(Webhook webhook, Outbox outbox, Lake lake, Authorization authorization, Fraud fraud, Chaos chaos) {

    public record Webhook(String secret) {}
    public record Outbox(int batchSize, long pollMs) {}
    public record Lake(String type, String bucket, String localDir, String s3Endpoint, String region,
                       long exportIntervalMs, int batchSize) {}
    public record Authorization(BigDecimal perTxnLimit, int velocityMaxPerMinute, double declineRiskThreshold,
                                Set<String> blockedMccs, Set<String> highRiskCountries) {}
    public record Fraud(double chaosFailureRate) {}
    public record Chaos(boolean enabled) {}
}
