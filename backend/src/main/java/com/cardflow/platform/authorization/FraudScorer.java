package com.cardflow.platform.authorization;

import java.math.BigDecimal;

public interface FraudScorer {
    /** @return fraud probability in [0,1] */
    double score(String cardToken, String mcc, BigDecimal amount, String country);
}
