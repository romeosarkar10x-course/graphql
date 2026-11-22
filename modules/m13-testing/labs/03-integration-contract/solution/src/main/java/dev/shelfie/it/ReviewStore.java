package dev.shelfie.it;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Reviews and books in PostgreSQL (schema m13_it). Ordering and rounding happen in SQL. */
@Repository
public class ReviewStore {

    public record Book(String id, String title) {}
    public record Review(String id, int rating, String body, String author) {}

    private final JdbcClient db;

    public ReviewStore(JdbcClient db) { this.db = db; }

    public Optional<Book> book(String id) {
        return db.sql("SELECT id, title FROM m13_it.book WHERE id = ?").param(id).query(Book.class).optional();
    }

    public List<Review> newestFirst(String bookId, int last) {
        return db.sql("SELECT id::text, rating, body, author FROM m13_it.review WHERE book_id = ? ORDER BY created_at DESC, id DESC LIMIT ?")
            .params(bookId, Math.max(0, last)).query(Review.class).list();
    }

    public Double average(String bookId) {
        return db.sql("SELECT ROUND(AVG(rating), 2)::float8 FROM m13_it.review WHERE book_id = ?")
            .param(bookId).query(Double.class).list().getFirst();   // one row; null without reviews
    }

    public Review add(String bookId, String author, int rating, String body) {
        return db.sql("INSERT INTO m13_it.review (book_id, author, rating, body) VALUES (?, ?, ?, ?) RETURNING id::text, rating, body, author")
            .params(bookId, author, rating, body).query(Review.class).single();
    }
}
