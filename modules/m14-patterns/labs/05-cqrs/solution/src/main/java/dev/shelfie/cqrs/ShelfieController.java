package dev.shelfie.cqrs;

import dev.shelfie.cqrs.ReviewCommands.UserError;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;

/** Queries read the read model; a review only the write side has is PENDING. */
@Controller
public class ShelfieController {
    public record Book(String id, String title) {}
    public record Review(String id, String status, int rating, String body, String author) {}
    public record CreateReviewInput(String bookId, String author, int rating, String body) {}
    public record CreateReviewPayload(Review review, Book book, List<UserError> userErrors) {}
    record Stats(int reviewCount, int ratingSum) {}

    private final JdbcClient jdbc;
    private final ReviewCommands commands;

    ShelfieController(JdbcClient jdbc, ReviewCommands commands) {
        this.jdbc = jdbc;
        this.commands = commands;
    }

    @QueryMapping public Book book(@Argument String id) {
        return jdbc.sql("SELECT id, title FROM m14_05.books WHERE id = ?").param(id).query(Book.class).optional().orElse(null);
    }

    @QueryMapping public Review review(@Argument String id) {
        return jdbc.sql("SELECT id, 'PUBLISHED' AS status, rating, body, author FROM m14_05.review_view WHERE id = ? "
                + "UNION ALL SELECT id, 'PENDING', rating, body, author FROM m14_05.reviews WHERE id = ? "
                + "AND id NOT IN (SELECT id FROM m14_05.review_view) LIMIT 1")
            .params(id, id).query(Review.class).optional().orElse(null);
    }

    Stats stats(Book book) {
        return jdbc.sql("SELECT review_count, rating_sum FROM m14_05.book_stats WHERE book_id = ?").param(book.id())
            .query(Stats.class).optional().orElse(new Stats(0, 0));
    }

    @SchemaMapping public int reviewCount(Book book) { return stats(book).reviewCount(); }

    @SchemaMapping public Double averageRating(Book book) {
        Stats s = stats(book);
        return s.reviewCount() == 0 ? null
            : BigDecimal.valueOf(s.ratingSum()).divide(BigDecimal.valueOf(s.reviewCount()), 2, RoundingMode.HALF_UP).doubleValue();
    }

    @SchemaMapping(typeName = "Book") public List<Review> reviews(Book book) {
        return jdbc.sql("SELECT id, 'PUBLISHED' AS status, rating, body, author FROM m14_05.review_view WHERE book_id = ? "
            + "ORDER BY published_at DESC, id").param(book.id()).query(Review.class).list();
    }

    @MutationMapping public CreateReviewPayload createReview(@Argument CreateReviewInput input) {
        var accepted = commands.create(input.bookId(), input.author(), input.rating(), input.body());
        if (!accepted.userErrors().isEmpty()) return new CreateReviewPayload(null, book(input.bookId()), accepted.userErrors());
        return new CreateReviewPayload(review(accepted.reviewId()), book(input.bookId()), List.of());
    }
}
