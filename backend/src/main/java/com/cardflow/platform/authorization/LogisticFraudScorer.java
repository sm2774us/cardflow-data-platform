package com.cardflow.platform.authorization;

import com.cardflow.platform.config.AppProperties;
import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Deterministic logistic-regression scorer (hand-set weights) standing in for a hosted ML model endpoint.
 * The FraudScorer port makes swapping in SageMaker/Bedrock/an in-house model a one-class change.
 */
@Component
public class LogisticFraudScorer implements FraudScorer {
    private static final Set<String> RISKY_MCC = Set.of("4829", "6012", "5967", "7995", "6051");
    private static final Set<String> HOME = Set.of("US", "CA", "GB", "MX", "BR", "AR", "CL", "CO", "PE");
    private final double chaosFailureRate;

    public LogisticFraudScorer(AppProperties props) { this.chaosFailureRate = props.fraud().chaosFailureRate(); }

    @Override
    public double score(String cardToken, String mcc, BigDecimal amount, String country) {
        if (chaosFailureRate > 0 && ThreadLocalRandom.current().nextDouble() < chaosFailureRate) {
            throw new IllegalStateException("fraud model endpoint unavailable (chaos)");
        }
        double a = Math.min(amount.doubleValue(), 20_000);
        double z = -4.2 + 0.0011 * a + (RISKY_MCC.contains(mcc) ? 1.6 : 0) + (HOME.contains(country) ? 0 : 1.4);
        return 1.0 / (1.0 + Math.exp(-z));
    }
}
