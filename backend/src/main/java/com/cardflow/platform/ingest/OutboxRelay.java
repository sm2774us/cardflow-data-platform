package com.cardflow.platform.ingest;

import static com.cardflow.platform.config.AmqpConfig.EXCHANGE;
import static com.cardflow.platform.config.AmqpConfig.ROUTING_KEY;

import com.cardflow.platform.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Transactional-outbox relay. Rows are claimed with FOR UPDATE SKIP LOCKED (safe with N replicas), published with
 * broker confirms, and only then marked SENT. A crash between publish and commit re-publishes the batch =
 * at-least-once; the idempotent consumer turns that into exactly-once effects.
 */
@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate tx;
    private final int batchSize;

    public OutboxRelay(NamedParameterJdbcTemplate jdbc, RabbitTemplate rabbit, TransactionTemplate tx, AppProperties props) {
        this.jdbc = jdbc; this.rabbit = rabbit; this.tx = tx; this.batchSize = props.outbox().batchSize();
    }

    @Scheduled(fixedDelayString = "${cardflow.outbox.poll-ms:250}")
    public void relay() {
        try {
            int n;
            do { n = relayBatch(); } while (n >= batchSize);
        } catch (Exception e) {
            log.warn("Outbox relay cycle failed (will retry): {}", e.toString());
        }
    }

    int relayBatch() {
        Integer sent = tx.execute(status -> {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT id, event_id, payload FROM outbox_event WHERE status = 'NEW' ORDER BY id LIMIT :n FOR UPDATE SKIP LOCKED",
                    new MapSqlParameterSource("n", batchSize));
            if (rows.isEmpty()) return 0;
            rabbit.invoke(ops -> {
                for (Map<String, Object> r : rows) {
                    var msg = MessageBuilder.withBody(((String) r.get("payload")).getBytes(StandardCharsets.UTF_8))
                            .setContentType("application/json").setMessageId((String) r.get("event_id"))
                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
                    ops.send(EXCHANGE, ROUTING_KEY, msg);
                }
                ops.waitForConfirmsOrDie(5_000);
                return null;
            });
            List<Object> ids = rows.stream().map(r -> r.get("id")).toList();
            jdbc.update("UPDATE outbox_event SET status='SENT', sent_at=now(), attempts=attempts+1 WHERE id IN (:ids)",
                    new MapSqlParameterSource("ids", ids));
            return rows.size();
        });
        return sent == null ? 0 : sent;
    }
}
