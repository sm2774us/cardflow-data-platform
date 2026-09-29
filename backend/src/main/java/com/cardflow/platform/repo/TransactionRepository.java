package com.cardflow.platform.repo;

import com.cardflow.platform.authorization.AuthorizationEngine.Decision;
import com.cardflow.platform.domain.CardEvent;
import com.cardflow.platform.domain.Records.PageResult;
import com.cardflow.platform.domain.TransactionView;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepository {
    static final RowMapper<TransactionView> MAPPER = (rs, i) -> new TransactionView(
            rs.getString("event_id"), rs.getString("event_type"), rs.getString("card_token"), rs.getString("merchant_name"),
            rs.getString("mcc"), rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("country"),
            rs.getTimestamp("occurred_at").toInstant(), rs.getString("decision"), rs.getString("decline_reason"),
            rs.getBigDecimal("risk_score"), rs.getTimestamp("created_at").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    public TransactionRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insert(CardEvent e, Decision d, String decision) {
        jdbc.update("""
                INSERT INTO card_transaction(event_id,event_type,card_token,merchant_name,mcc,amount,currency,country,
                  occurred_at,decision,decline_reason,risk_score)
                VALUES (:id,:type,:token,:merchant,:mcc,:amount,:cur,:country,:at,:decision,:reason,:risk)
                ON CONFLICT (event_id) DO NOTHING""",
                new MapSqlParameterSource().addValue("id", e.eventId()).addValue("type", e.eventType())
                        .addValue("token", e.cardToken()).addValue("merchant", e.merchantName()).addValue("mcc", e.mcc())
                        .addValue("amount", e.amount()).addValue("cur", e.currency()).addValue("country", e.country())
                        .addValue("at", Timestamp.from(e.occurredAt())).addValue("decision", decision)
                        .addValue("reason", d.reasons().isEmpty() ? null : String.join(",", d.reasons()))
                        .addValue("risk", d.riskScore()));
    }

    public PageResult<TransactionView> search(String decision, String q, int page, int size) {
        var where = new StringBuilder(" WHERE 1=1");
        var p = new MapSqlParameterSource().addValue("limit", size).addValue("offset", (long) page * size);
        if (decision != null && !decision.isBlank()) { where.append(" AND decision = :decision"); p.addValue("decision", decision); }
        if (q != null && !q.isBlank()) {
            where.append(" AND (merchant_name ILIKE :q OR card_token ILIKE :q OR event_id = :exact)");
            p.addValue("q", "%" + q.trim() + "%").addValue("exact", q.trim());
        }
        Long total = jdbc.queryForObject("SELECT count(*) FROM card_transaction" + where, p, Long.class);
        List<TransactionView> items = jdbc.query(
                "SELECT * FROM card_transaction" + where + " ORDER BY created_at DESC, event_id LIMIT :limit OFFSET :offset", p, MAPPER);
        return new PageResult<>(items, total == null ? 0 : total, page, size);
    }

    /** Claims un-exported rows with SKIP LOCKED so multiple replicas can export concurrently without overlap. */
    public List<TransactionView> claimUnexported(int limit) {
        return jdbc.query("""
                SELECT * FROM card_transaction WHERE lake_batch_id IS NULL
                ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED""",
                new MapSqlParameterSource("limit", limit), MAPPER);
    }

    public void markExported(List<TransactionView> rows, long batchId) {
        List<String> ids = rows.stream().map(TransactionView::eventId).toList();
        jdbc.update("UPDATE card_transaction SET lake_batch_id = :b WHERE event_id IN (:ids)",
                new MapSqlParameterSource().addValue("b", batchId).addValue("ids", ids));
    }
}
