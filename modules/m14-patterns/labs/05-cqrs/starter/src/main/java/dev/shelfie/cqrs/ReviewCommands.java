package dev.shelfie.cqrs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Today the command writes the review and updates the read model itself, in one transaction: simple, immediate,
 * and every reader of reviews is coupled to this method. The outbox table already exists (db/schema.sql).
 *
 * TODO the command stores the review and a ReviewCreated event (outbox) only; a relay publishes the outbox to the
 * Kafka topic ${shelfie.topic}, keyed by book; a projection (a @KafkaListener) is the read model's only writer,
 * and applies each event once.
 */
@Service
public class ReviewCommands {
    public record UserError(List<String> field, String message) {}
    public record Accepted(String reviewId, List<UserError> userErrors) {}

    private final JdbcClient jdbc;

    ReviewCommands(JdbcClient jdbc, SchemaSetup schema) {
        this.jdbc = jdbc;
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
        jdbc.sql("INSERT INTO m14_05.reviews (id, book_id, author, rating, body) VALUES (?, ?, ?, ?, ?)")
            .params(id, bookId, author, rating, body.trim()).update();
        jdbc.sql("INSERT INTO m14_05.review_view (id, book_id, author, rating, body) VALUES (?, ?, ?, ?, ?)")
            .params(id, bookId, author, rating, body.trim()).update();
        jdbc.sql("UPDATE m14_05.book_stats SET review_count = review_count + 1, rating_sum = rating_sum + ? WHERE book_id = ?")
            .params(rating, bookId).update();
        return new Accepted(id, List.of());
    }
}
