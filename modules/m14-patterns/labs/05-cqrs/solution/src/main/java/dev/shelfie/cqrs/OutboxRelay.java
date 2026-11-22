package dev.shelfie.cqrs;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes unpublished outbox rows to Kafka every 500 ms, oldest first, keyed by book so one book's events stay
 * in order. A row is marked published only after the broker acknowledged it; a crash in between publishes it again
 * on the next run, which is why the projection must ignore repeats. SKIP LOCKED lets several relays share the work.
 */
@Component
public class OutboxRelay {
    record Row(UUID eventId, String aggregateId, String payload) {}

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    OutboxRelay(JdbcClient jdbc, KafkaTemplate<String, String> kafka, @Value("${shelfie.topic}") String topic, SchemaSetup schema) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.topic = topic;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void relay() throws Exception {
        List<Row> rows = jdbc.sql("SELECT event_id, aggregate_id, payload::text FROM m14_05.outbox WHERE published_at IS NULL "
            + "ORDER BY created_at LIMIT 100 FOR UPDATE SKIP LOCKED").query(Row.class).list();
        for (Row row : rows) {
            kafka.send(topic, row.aggregateId(), row.payload()).get(5, TimeUnit.SECONDS);
            jdbc.sql("UPDATE m14_05.outbox SET published_at = now() WHERE event_id = ?").param(row.eventId()).update();
        }
    }
}
