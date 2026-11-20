package dev.shelfie.writes;

import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sql.DataSource;

/**
 * Lab 4.4: three mutations that stay correct when clients retry, race and wait.
 * The test calls {@link #graphQL(DataSource)}; keep that signature.
 */
public final class Writes {
    private Writes() {}

    static final String VIEWER = "u1";
    static final ExecutorService JOBS = Executors.newVirtualThreadPerTaskExecutor();

    public static GraphQL graphQL(DataSource db) {
        String sdl;
        try (InputStream in = Writes.class.getResourceAsStream("/writes.graphqls")) {
            sdl = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        var wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", t -> t
                        .dataFetcher("review", env -> review(db, Integer.parseInt(env.getArgument("id"))))
                        .dataFetcher("club", env -> club(db, env.getArgument("id")))
                        .dataFetcher("job", env -> job(db, env.getArgument("id")))
                        .dataFetcher("reviewCount", env -> one(db, "SELECT COUNT(*) FROM reviews")))
                .type("Mutation", t -> t
                        .dataFetcher("createReview", env -> createReview(db, input(env)))
                        .dataFetcher("renameClub", env -> renameClub(db, input(env)))
                        .dataFetcher("importBooks", env -> importBooks(db, input(env))))
                .build();
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), wiring)).build();
    }

    static Map<String, Object> input(DataFetchingEnvironment env) {
        return env.getArgument("input");
    }

    static Map<String, Object> error(List<String> field, String message, String code) {
        return Map.of("field", field, "message", message, "code", code);
    }

    // ---------------------------------------------------------------- idempotency key

    /**
     * TODO 1: first draft. Check, then act: two requests can both see "no such key" and both insert.
     * Make it safe under 50 concurrent identical requests, replay the first result (replayed: true),
     * and reject the same key with a different input (KEY_REUSED).
     */
    static Map<String, Object> createReview(DataSource db, Map<String, Object> in) throws SQLException {
        String key = (String) in.get("idempotencyKey");
        try (Connection c = db.getConnection();
             PreparedStatement q = c.prepareStatement("SELECT review_id FROM idempotency_keys WHERE idem_key = ?")) {
            q.setString(1, key);
            try (ResultSet r = q.executeQuery()) {
                if (r.next()) return Map.of("review", review(db, r.getInt(1)), "replayed", true, "userErrors", List.of());
            }
        }
        int id;
        try (Connection c = db.getConnection(); PreparedStatement ins = c.prepareStatement(
                "INSERT INTO reviews (book_id, author_id, rating, body) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ins.setString(1, (String) in.get("bookId"));
            ins.setString(2, VIEWER);
            ins.setInt(3, (Integer) in.get("rating"));
            ins.setString(4, (String) in.get("body"));
            ins.executeUpdate();
            try (ResultSet k = ins.getGeneratedKeys()) { k.next(); id = k.getInt(1); }
        }
        update(db, "INSERT INTO idempotency_keys VALUES (?, ?, ?)", key, "", id);
        return Map.of("review", review(db, id), "replayed", false, "userErrors", List.of());
    }

    // ---------------------------------------------------------------- optimistic locking

    /** TODO 2: this ignores expectedVersion, so the last writer silently wins. Return CONFLICT instead. */
    static Map<String, Object> renameClub(DataSource db, Map<String, Object> in) throws SQLException {
        String id = (String) in.get("clubId");
        update(db, "UPDATE clubs SET name = ?, version = version + 1 WHERE id = ?", in.get("name"), id);
        return Map.of("club", club(db, id), "userErrors", List.of());
    }

    // ---------------------------------------------------------------- long-running job

    /** TODO 3: this does the whole import inside the request. Record the job, run it on JOBS, answer at once. */
    static Map<String, Object> importBooks(DataSource db, Map<String, Object> in) throws SQLException {
        @SuppressWarnings("unchecked") List<String> isbns = (List<String>) in.get("isbns");
        String jobId = "job-" + UUID.randomUUID().toString().substring(0, 8);
        update(db, "INSERT INTO jobs VALUES (?, 'RUNNING', ?, 0)", jobId, isbns.size());
        for (String isbn : isbns) {
            try { Thread.sleep(100); } catch (InterruptedException e) { throw new IllegalStateException(e); }
            if (isbn.matches("97[89]\\d{10}")) {
                update(db, "INSERT INTO books VALUES (?, ?)", "isbn-" + isbn, isbn);
                update(db, "UPDATE jobs SET imported = imported + 1 WHERE id = ?", jobId);
            }
        }
        update(db, "UPDATE jobs SET status = 'DONE' WHERE id = ?", jobId);
        return Map.of("job", job(db, jobId));
    }

    // ---------------------------------------------------------------- reads

    static Map<String, Object> review(DataSource db, int id) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement q = c.prepareStatement("SELECT id, book_id, rating, body FROM reviews WHERE id = ?")) {
            q.setInt(1, id);
            try (ResultSet r = q.executeQuery()) {
                return r.next() ? Map.of("id", String.valueOf(r.getInt(1)), "bookId", r.getString(2), "rating", r.getInt(3), "body", r.getString(4)) : null;
            }
        }
    }

    static Map<String, Object> club(DataSource db, String id) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement q = c.prepareStatement("SELECT id, name, version FROM clubs WHERE id = ?")) {
            q.setString(1, id);
            try (ResultSet r = q.executeQuery()) {
                return r.next() ? Map.of("id", r.getString(1), "name", r.getString(2), "version", r.getInt(3)) : null;
            }
        }
    }

    static Map<String, Object> job(DataSource db, String id) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement q = c.prepareStatement("SELECT id, status, total, imported FROM jobs WHERE id = ?")) {
            q.setString(1, id);
            try (ResultSet r = q.executeQuery()) {
                return r.next() ? Map.of("id", r.getString(1), "status", r.getString(2), "total", r.getInt(3), "imported", r.getInt(4)) : null;
            }
        }
    }

    static int one(DataSource db, String sql) throws SQLException {
        try (Connection c = db.getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            r.next();
            return r.getInt(1);
        }
    }

    static void update(DataSource db, String sql, Object... args) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            p.executeUpdate();
        }
    }
}
