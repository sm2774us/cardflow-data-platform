package com.cardflow.platform.authorization;

import com.cardflow.platform.config.AppProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Real-time authorization decisioning: ordered deterministic rules + ML risk score. Pure and unit-testable. */
@Service
public class AuthorizationEngine {
    public record Decision(boolean approved, List<String> reasons, double riskScore, boolean degraded) {}

    private final AppProperties.Authorization cfg;
    private final ResilientFraudScorer scorer;
    private final VelocityTracker velocity;

    public AuthorizationEngine(AppProperties props, ResilientFraudScorer scorer, VelocityTracker velocity) {
        this.cfg = props.authorization(); this.scorer = scorer; this.velocity = velocity;
    }

    public Decision decide(String cardToken, String mcc, BigDecimal amount, String country) {
        List<String> reasons = new ArrayList<>();
        if (cfg.blockedMccs().contains(mcc)) reasons.add("BLOCKED_MCC");
        if (cfg.highRiskCountries().contains(country)) reasons.add("HIGH_RISK_COUNTRY");
        if (amount.compareTo(cfg.perTxnLimit()) > 0) reasons.add("OVER_TXN_LIMIT");
        if (velocity.recordAndCount(cardToken, System.currentTimeMillis()) > cfg.velocityMaxPerMinute()) reasons.add("VELOCITY_EXCEEDED");

        var s = scorer.score(cardToken, mcc, amount, country);
        if (s.value() >= cfg.declineRiskThreshold()) reasons.add("HIGH_FRAUD_RISK");
        double risk = BigDecimal.valueOf(s.value()).setScale(4, RoundingMode.HALF_UP).doubleValue();
        return new Decision(reasons.isEmpty(), List.copyOf(reasons), risk, s.degraded());
    }
}
