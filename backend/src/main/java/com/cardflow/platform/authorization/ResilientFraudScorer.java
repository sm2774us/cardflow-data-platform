package com.cardflow.platform.authorization;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** Circuit-breaker facade: when the model is failing, authorizations keep flowing on a conservative rules fallback. */
@Service
public class ResilientFraudScorer {
    public record Score(double value, boolean degraded) {}

    private final FraudScorer delegate;

    public ResilientFraudScorer(FraudScorer delegate) { this.delegate = delegate; }

    @CircuitBreaker(name = "fraudScorer", fallbackMethod = "fallback")
    public Score score(String cardToken, String mcc, BigDecimal amount, String country) {
        return new Score(delegate.score(cardToken, mcc, amount, country), false);
    }

    @SuppressWarnings("unused")
    Score fallback(String cardToken, String mcc, BigDecimal amount, String country, Throwable t) {
        return new Score(amount.compareTo(new BigDecimal("1500")) > 0 ? 0.5 : 0.1, true);
    }
}
