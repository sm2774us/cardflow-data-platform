package com.cardflow.platform.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.cardflow.platform.authorization.*;
import com.cardflow.platform.config.AppProperties;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthorizationEngineTest {
    private static AuthorizationEngine engine(FraudScorer scorer) {
        var auth = new AppProperties.Authorization(new BigDecimal("5000"), 3, 0.85, Set.of("7995"), Set.of("KP"));
        var props = new AppProperties(null, null, null, auth, new AppProperties.Fraud(0), new AppProperties.Chaos(false));
        return new AuthorizationEngine(props, new ResilientFraudScorer(scorer), new VelocityTracker());
    }

    @Test void approvesLowRiskTransaction() {
        var d = engine((t, m, a, c) -> 0.05).decide("tok_A", "5411", new BigDecimal("25"), "US");
        assertThat(d.approved()).isTrue();
        assertThat(d.reasons()).isEmpty();
    }

    @Test void declinesWithAllApplicableReasons() {
        var d = engine((t, m, a, c) -> 0.95).decide("tok_B", "7995", new BigDecimal("9000"), "KP");
        assertThat(d.approved()).isFalse();
        assertThat(d.reasons()).containsExactly("BLOCKED_MCC", "HIGH_RISK_COUNTRY", "OVER_TXN_LIMIT", "HIGH_FRAUD_RISK");
    }

    @Test void velocityLimitTripsOnFourthRapidTransaction() {
        var e = engine((t, m, a, c) -> 0.01);
        for (int i = 0; i < 3; i++) assertThat(e.decide("tok_C", "5411", BigDecimal.TEN, "US").approved()).isTrue();
        assertThat(e.decide("tok_C", "5411", BigDecimal.TEN, "US").reasons()).containsExactly("VELOCITY_EXCEEDED");
    }
}
