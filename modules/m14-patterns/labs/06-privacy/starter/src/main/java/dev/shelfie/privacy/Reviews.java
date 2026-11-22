package dev.shelfie.privacy;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reviews. The table has a deleted_at column nobody uses yet. */
@Service
public class Reviews {
    public record Review(String id, String bookId, String authorId, int rating, String body) {}

    private static final Logger log = LoggerFactory.getLogger(Reviews.class);
    static final String COLUMNS = "SELECT id, book_id, author_id, rating, body FROM m14_06.reviews ";
    private final JdbcClient jdbc;

    Reviews(JdbcClient jdbc, SchemaSetup schema) {
        this.jdbc = jdbc;
    }

    public Review byId(String id) {
        return jdbc.sql(COLUMNS + "WHERE id = ? AND deleted_at IS NULL").param(id).query(Review.class).optional().orElse(null);
    }

    public List<Review> ofBook(String bookId) {
        return jdbc.sql(COLUMNS + "WHERE book_id = ? AND deleted_at IS NULL ORDER BY created_at DESC, id DESC").param(bookId).query(Review.class).list();
    }

    public List<Review> ofAuthor(String authorId) {
        return jdbc.sql(COLUMNS + "WHERE author_id = ? AND deleted_at IS NULL ORDER BY created_at DESC, id DESC").param(authorId).query(Review.class).list();
    }

    public Review create(String bookId, String authorId, int rating, String body) {
        String id = "r" + jdbc.sql("SELECT nextval('m14_06.review_ids')").query(Long.class).single();
        jdbc.sql("INSERT INTO m14_06.reviews (id, book_id, author_id, rating, body) VALUES (?, ?, ?, ?, ?)")
            .params(id, bookId, authorId, rating, body).update();
        log.info("review {} created by {} for {}: {}", id, authorId, bookId, body);
        return byId(id);
    }

    /** The review's author, deleted or not; null when there is no such review. */
    public String authorOf(String id) {
        return jdbc.sql("SELECT author_id FROM m14_06.reviews WHERE id = ?").param(id).query(String.class).optional().orElse(null);
    }

    public void softDelete(String id) {
        jdbc.sql("DELETE FROM m14_06.reviews WHERE id = ?").param(id).update();   // TODO soft delete: restorable for 30 days
        log.info("review {} deleted", id);
    }

    public Review restore(String id) {
        return byId(id);   // TODO
    }

    @Transactional
    public int purge(int olderThanDays) {
        return 0;   // TODO retention
    }

    /** Retention, every night at 03:00: nobody has to remember to call purgeDeletedReviews. */
    @Scheduled(cron = "0 0 3 * * *")
    public void nightly() {
        purge(30);
    }
}
