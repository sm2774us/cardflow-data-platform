package com.cardflow.platform.processing;

import com.cardflow.platform.domain.CardEvent;
import com.cardflow.platform.domain.Records.DqRuleInfo;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Data-quality "promotion gate": an event is promoted to the curated table (and thus the lake) only if every rule passes.
 * Failures are quarantined with the failing rule codes, so quality is measurable and remediable, not silently dropped.
 */
@Component
public class DataQualityGate {
    public record Rule(String code, String description, Predicate<CardEvent> passes) {}

    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());
    private static final Set<String> EVENT_TYPES = Set.of("AUTHORIZATION", "CLEARING", "REVERSAL");
    private static final Pattern MCC = Pattern.compile("\\d{4}");
    private static final Pattern TOKEN = Pattern.compile("tok_[A-Za-z0-9]{8,32}");
    private static final Pattern PAN_LIKE = Pattern.compile("\\d{13,19}");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    private final List<Rule> rules = List.of(
            new Rule("REQUIRED_FIELDS", "All mandatory fields are present",
                    e -> e.eventId() != null && e.eventType() != null && e.cardToken() != null && e.merchantName() != null
                            && !e.merchantName().isBlank() && e.mcc() != null && e.amount() != null && e.currency() != null
                            && e.country() != null && e.occurredAt() != null),
            new Rule("SCHEMA_VERSION", "schemaVersion equals 1", e -> Integer.valueOf(1).equals(e.schemaVersion())),
            new Rule("EVENT_TYPE_KNOWN", "eventType is AUTHORIZATION, CLEARING or REVERSAL",
                    e -> e.eventType() != null && EVENT_TYPES.contains(e.eventType())),
            new Rule("AMOUNT_RANGE", "0 < amount <= 1,000,000",
                    e -> e.amount() != null && e.amount().signum() > 0 && e.amount().compareTo(MAX_AMOUNT) <= 0),
            new Rule("CURRENCY_ISO4217", "currency is a valid ISO-4217 code", e -> validCurrency(e.currency())),
            new Rule("COUNTRY_ISO3166", "country is a valid ISO-3166 alpha-2 code",
                    e -> e.country() != null && COUNTRIES.contains(e.country())),
            new Rule("MCC_FORMAT", "mcc is exactly four digits", e -> e.mcc() != null && MCC.matcher(e.mcc()).matches()),
            new Rule("TOKENIZED_CARD", "cardToken matches tok_* format",
                    e -> e.cardToken() != null && TOKEN.matcher(e.cardToken()).matches()),
            new Rule("NO_PAN_LEAK", "No PAN-like digit run in cardToken or merchantName (PCI guard)",
                    e -> !containsPan(e.cardToken()) && !containsPan(e.merchantName())),
            new Rule("TIMESTAMP_SANE", "occurredAt not >5 min in the future and not >30 days old", e -> {
                if (e.occurredAt() == null) return false;
                Instant now = Instant.now();
                return !e.occurredAt().isAfter(now.plus(Duration.ofMinutes(5))) && e.occurredAt().isAfter(now.minus(Duration.ofDays(30)));
            }));

    public List<String> evaluate(CardEvent event) {
        return rules.stream().filter(r -> !safe(r, event)).map(Rule::code).toList();
    }

    public List<DqRuleInfo> describe() { return rules.stream().map(r -> new DqRuleInfo(r.code(), r.description())).toList(); }

    private static boolean safe(Rule r, CardEvent e) {
        try { return r.passes().test(e); } catch (RuntimeException ex) { return false; }
    }

    private static boolean validCurrency(String c) {
        if (c == null) return false;
        try { Currency.getInstance(c); return c.length() == 3; } catch (IllegalArgumentException ex) { return false; }
    }

    private static boolean containsPan(String s) {
        return s != null && PAN_LIKE.matcher(s.replaceAll("[ -]", "")).find();
    }
}
