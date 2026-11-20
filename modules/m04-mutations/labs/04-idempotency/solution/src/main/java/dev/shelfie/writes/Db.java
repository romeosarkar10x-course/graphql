package dev.shelfie.writes;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;

/**
 * Given; do not edit. A fresh in-memory H2 database per call, with the tables lab 4.4 needs.
 * JPA's @Version does exactly what the clubs.version column does here, generated for you (Module 6).
 */
public final class Db {
    private Db() {}
    private static final AtomicInteger SEQ = new AtomicInteger();

    public static DataSource fresh() {
        JdbcDataSource ds = new JdbcDataSource();
        // LOCK_TIMEOUT: a transaction waiting on another's row lock waits up to 10 s instead of failing
        ds.setURL("jdbc:h2:mem:writes" + SEQ.incrementAndGet() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("""
                    CREATE TABLE books (id VARCHAR PRIMARY KEY, isbn VARCHAR);
                    CREATE TABLE clubs (id VARCHAR PRIMARY KEY, name VARCHAR NOT NULL, version INT NOT NULL);
                    CREATE TABLE reviews (id INT AUTO_INCREMENT PRIMARY KEY, book_id VARCHAR NOT NULL,
                                          author_id VARCHAR NOT NULL, rating INT NOT NULL, body VARCHAR NOT NULL);
                    CREATE TABLE idempotency_keys (idem_key VARCHAR PRIMARY KEY, request_hash VARCHAR NOT NULL,
                                                   review_id INT);
                    CREATE TABLE jobs (id VARCHAR PRIMARY KEY, status VARCHAR NOT NULL, total INT NOT NULL,
                                       imported INT NOT NULL);
                    INSERT INTO books VALUES ('b1', '9780441478125'), ('b2', '9781635575637');
                    INSERT INTO clubs VALUES ('c1', 'The Midnight Readers', 0), ('c2', 'Slow Burn Mysteries', 0);
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return ds;
    }
}
