package dev.shelfie.cqrs;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The read side's only writer. Applies each ReviewCreated event once: the event id goes into processed_events in
 * the same transaction as the read-model change, so a repeated delivery finds it there and changes nothing.
 */
@Component
public class ReviewProjection {
    private final JdbcClient jdbc;
    private final JsonMapper json;

    ReviewProjection(JdbcClient jdbc, JsonMapper json, SchemaSetup schema) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @KafkaListener(topics = "${shelfie.topic}", groupId = "${shelfie.topic}-projection")
    @Transactional
    public void on(String payload) {
        JsonNode e = json.readTree(payload);
        if (!"ReviewCreated".equals(e.path("type").asString())) return;
        int fresh = jdbc.sql("INSERT INTO m14_05.processed_events (event_id) VALUES (?) ON CONFLICT DO NOTHING")
            .param(UUID.fromString(e.get("eventId").asString())).update();
        if (fresh == 0) return;   // seen before: at-least-once delivery, applied at most once
        String bookId = e.get("bookId").asString();
        int rating = e.get("rating").asInt();
        jdbc.sql("INSERT INTO m14_05.review_view (id, book_id, author, rating, body) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING")
            .params(e.get("reviewId").asString(), bookId, e.get("author").asString(), rating, e.get("body").asString()).update();
        jdbc.sql("INSERT INTO m14_05.book_stats VALUES (?, 1, ?) ON CONFLICT (book_id) DO UPDATE "
            + "SET review_count = book_stats.review_count + 1, rating_sum = book_stats.rating_sum + EXCLUDED.rating_sum")
            .params(bookId, rating).update();
    }
}
