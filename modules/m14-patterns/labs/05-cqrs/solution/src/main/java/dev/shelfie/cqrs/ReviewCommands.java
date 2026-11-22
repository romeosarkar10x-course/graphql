package dev.shelfie.cqrs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * The write side. A command checks its rules, then stores the review and its event in ONE transaction: either both
 * exist or neither. Publishing to Kafka here instead would lose the event when the broker is down after the commit,
 * or announce a review whose transaction then rolled back.
 */
@Service
public class ReviewCommands {
    public record UserError(List<String> field, String message) {}
    public record Accepted(String reviewId, List<UserError> userErrors) {}

    private final JdbcClient jdbc;
    private final JsonMapper json;

    ReviewCommands(JdbcClient jdbc, JsonMapper json, SchemaSetup schema) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Transactional
    public Accepted create(String bookId, String author, int rating, String body) {
        List<UserError> errors = new ArrayList<>();
        boolean book = jdbc.sql("SELECT count(*) FROM m14_05.books WHERE id = ?").param(bookId).query(Integer.class).single() > 0;
        if (!book) errors.add(new UserError(List.of("input", "bookId"), "No book " + bookId));
        if (rating < 1 || rating > 5) errors.add(new UserError(List.of("input", "rating"), "Rating must be 1 to 5"));
        if (body.isBlank()) errors.add(new UserError(List.of("input", "body"), "Say something"));
        if (!errors.isEmpty()) return new Accepted(null, errors);

        String id = "r-" + UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        jdbc.sql("INSERT INTO m14_05.reviews (id, book_id, author, rating, body) VALUES (?, ?, ?, ?, ?)")
            .params(id, bookId, author, rating, body.trim()).update();
        String payload = json.writeValueAsString(Map.of("eventId", eventId.toString(), "type", "ReviewCreated",
            "reviewId", id, "bookId", bookId, "author", author, "rating", rating, "body", body.trim()));
        jdbc.sql("INSERT INTO m14_05.outbox (event_id, aggregate_id, type, payload) VALUES (?, ?, 'ReviewCreated', ?::jsonb)")
            .params(eventId, bookId, payload).update();
        return new Accepted(id, List.of());
    }
}
