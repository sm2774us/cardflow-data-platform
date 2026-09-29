package com.cardflow.platform.lake;

import com.cardflow.platform.config.AppProperties;
import com.cardflow.platform.domain.TransactionView;
import com.cardflow.platform.repo.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Micro-batch exporter to the data lake: NDJSON+gzip, Hive-style partitions (dt=/hour=) that Glue/Athena partition-projection
 * and Snowflake external stages/Snowpipe consume directly. Claim -> write -> mark is one transaction, so a failed write
 * leaves rows unclaimed for the next cycle (at-least-once; batch ids make objects traceable).
 */
@Component
public class LakeExportJob {
    private static final Logger log = LoggerFactory.getLogger(LakeExportJob.class);

    private final TransactionRepository txns;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final LakeSink sink;
    private final ObjectMapper snakeMapper;
    private final MeterRegistry metrics;
    private final int batchSize;

    public LakeExportJob(TransactionRepository txns, JdbcTemplate jdbc, TransactionTemplate tx, LakeSink sink,
                         ObjectMapper mapper, MeterRegistry metrics, AppProperties props) {
        this.txns = txns; this.jdbc = jdbc; this.tx = tx; this.sink = sink; this.metrics = metrics;
        this.snakeMapper = mapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.batchSize = props.lake().batchSize();
    }

    @Scheduled(fixedDelayString = "${cardflow.lake.export-interval-ms:30000}", initialDelay = 10_000)
    public void run() {
        try {
            int n;
            do { n = exportOnce(); } while (n >= batchSize);
        } catch (Exception e) {
            metrics.counter("cardflow.lake.export.failures").increment();
            log.error("Lake export failed; rows remain unclaimed and will be retried", e);
        }
    }

    /** @return number of rows exported in this batch. */
    public int exportOnce() {
        Integer n = tx.execute(status -> {
            List<TransactionView> rows = txns.claimUnexported(batchSize);
            if (rows.isEmpty()) return 0;
            try {
                Long batchId = jdbc.queryForObject("INSERT INTO lake_batch(record_count) VALUES (?) RETURNING id", Long.class, rows.size());
                ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
                String key = String.format("transactions/dt=%s/hour=%02d/batch-%010d.ndjson.gz", now.toLocalDate(), now.getHour(), batchId);
                byte[] data = gzipNdjson(rows);
                String location = sink.write(key, data);
                jdbc.update("UPDATE lake_batch SET object_key = ?, bytes = ? WHERE id = ?", location, data.length, batchId);
                txns.markExported(rows, batchId);
                metrics.counter("cardflow.lake.exported.records").increment(rows.size());
                log.info("Exported {} records to {}", rows.size(), location);
                return rows.size();
            } catch (Exception e) {
                throw new IllegalStateException("lake export failed", e);
            }
        });
        return n == null ? 0 : n;
    }

    private byte[] gzipNdjson(List<TransactionView> rows) throws Exception {
        var bos = new ByteArrayOutputStream(rows.size() * 220);
        try (var gz = new GZIPOutputStream(bos)) {
            for (TransactionView r : rows) {
                gz.write(snakeMapper.writeValueAsBytes(r));
                gz.write('\n');
            }
        }
        return bos.toByteArray();
    }

    @SuppressWarnings("unused")
    private static final java.nio.charset.Charset UTF8 = StandardCharsets.UTF_8;
}
