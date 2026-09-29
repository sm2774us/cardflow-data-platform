package com.cardflow.platform.repo;

import com.cardflow.platform.domain.Records.*;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Operational tables: outbox, quarantine, dead letters, stats. Plain SQL — set-based and index-friendly. */
@Repository
public class OpsRepository {
    private final JdbcTemplate jdbc;

    public OpsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** @return true if newly stored, false if the event id was already accepted (duplicate webhook delivery). */
    public boolean insertOutbox(String eventId, String payload) {
        return jdbc.update("INSERT INTO outbox_event(event_id, payload) VALUES (?, ?) ON CONFLICT (event_id) DO NOTHING",
                eventId, payload) == 1;
    }

    public void saveQuarantine(String eventId, String raw, String failedRules) {
        jdbc.update("INSERT INTO dq_quarantine(event_id, raw_payload, failed_rules) VALUES (?,?,?)", eventId, raw, failedRules);
    }

    public void saveDeadLetter(String eventId, String raw, String error) {
        jdbc.update("INSERT INTO dead_letter(event_id, raw_payload, error) VALUES (?,?,?)", eventId, raw,
                error.length() > 500 ? error.substring(0, 500) : error);
    }

    public List<QuarantineItem> quarantine(int limit) {
        return jdbc.query("SELECT * FROM dq_quarantine ORDER BY id DESC LIMIT ?",
                (rs, i) -> new QuarantineItem(rs.getLong("id"), rs.getString("event_id"), rs.getString("raw_payload"),
                        rs.getString("failed_rules"), rs.getTimestamp("created_at").toInstant(), rs.getBoolean("replayed")), limit);
    }

    public List<DeadLetterItem> deadLetters(int limit) {
        return jdbc.query("SELECT * FROM dead_letter ORDER BY id DESC LIMIT ?",
                (rs, i) -> new DeadLetterItem(rs.getLong("id"), rs.getString("event_id"), rs.getString("raw_payload"),
                        rs.getString("error"), rs.getTimestamp("created_at").toInstant(), rs.getBoolean("replayed")), limit);
    }

    /** Re-drive a parked event: clear its idempotency record and re-enqueue via the outbox (optionally with a corrected payload). */
    @Transactional
    public boolean replay(String table, long id, String correctedPayload) {
        if (!table.equals("dq_quarantine") && !table.equals("dead_letter")) throw new IllegalArgumentException(table);
        var rows = jdbc.queryForList("SELECT event_id, raw_payload FROM " + table + " WHERE id = ? AND replayed = FALSE FOR UPDATE", id);
        if (rows.isEmpty()) return false;
        String eventId = (String) rows.get(0).get("event_id");
        String payload = correctedPayload != null && !correctedPayload.isBlank() ? correctedPayload : (String) rows.get(0).get("raw_payload");
        jdbc.update("DELETE FROM processed_event WHERE event_id = ?", eventId);
        jdbc.update("DELETE FROM outbox_event WHERE event_id = ?", eventId);
        jdbc.update("INSERT INTO outbox_event(event_id, payload) VALUES (?, ?)", eventId, payload);
        jdbc.update("UPDATE " + table + " SET replayed = TRUE WHERE id = ?", id);
        return true;
    }

    public PipelineStats stats(long duplicatesDropped) {
        long outbox = count("SELECT count(*) FROM outbox_event WHERE status='NEW'");
        long approved = count("SELECT count(*) FROM card_transaction WHERE decision <> 'DECLINED'");
        long declined = count("SELECT count(*) FROM card_transaction WHERE decision='DECLINED'");
        long quarantined = count("SELECT count(*) FROM dq_quarantine");
        long total = approved + declined;
        double pass = total + quarantined == 0 ? 1.0 : (double) total / (total + quarantined);
        var throughput = jdbc.query("""
                SELECT date_trunc('minute', created_at) AS m, count(*) AS n, count(*) FILTER (WHERE decision='DECLINED') AS d
                FROM card_transaction WHERE created_at > now() - interval '30 minutes' GROUP BY 1 ORDER BY 1""",
                (rs, i) -> new ThroughputPoint(rs.getTimestamp("m").toInstant(), rs.getLong("n"), rs.getLong("d")));
        return new PipelineStats(outbox, total, approved, declined, quarantined,
                count("SELECT count(*) FROM dead_letter"), duplicatesDropped,
                count("SELECT count(*) FROM lake_batch"), count("SELECT COALESCE(sum(record_count),0) FROM lake_batch"),
                count("SELECT count(*) FROM card_transaction WHERE lake_batch_id IS NULL"), pass, throughput);
    }

    private long count(String sql) { Long v = jdbc.queryForObject(sql, Long.class); return v == null ? 0 : v; }
}
