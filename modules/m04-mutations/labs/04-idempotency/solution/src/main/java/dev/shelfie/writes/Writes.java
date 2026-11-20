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
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
     * Claim the key first, inside the same transaction as the write. The primary key on idem_key makes
     * the database the referee: of 50 concurrent inserts exactly one wins; the others wait for its
     * commit, then fail with a constraint violation and replay the winner's result.
     */
    static Map<String, Object> createReview(DataSource db, Map<String, Object> in) throws SQLException {
        String key = (String) in.get("idempotencyKey");
        String hash = in.get("bookId") + "|" + in.get("rating") + "|" + in.get("body");
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement claim = c.prepareStatement("INSERT INTO idempotency_keys (idem_key, request_hash) VALUES (?, ?)")) {
                    claim.setString(1, key);
                    claim.setString(2, hash);
                    claim.executeUpdate();
                }
                int id;
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO reviews (book_id, author_id, rating, body) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ins.setString(1, (String) in.get("bookId"));
                    ins.setString(2, VIEWER);
                    ins.setInt(3, (Integer) in.get("rating"));
                    ins.setString(4, (String) in.get("body"));
                    ins.executeUpdate();
                    try (ResultSet k = ins.getGeneratedKeys()) { k.next(); id = k.getInt(1); }
                }
                try (PreparedStatement link = c.prepareStatement("UPDATE idempotency_keys SET review_id = ? WHERE idem_key = ?")) {
                    link.setInt(1, id);
                    link.setString(2, key);
                    link.executeUpdate();
                }
                c.commit();
                return Map.of("review", review(db, id), "replayed", false, "userErrors", List.of());
            } catch (SQLIntegrityConstraintViolationException duplicate) {
                c.rollback();
            }
        }
        // Someone else holds (held) this key: replay their result, if it was the same request.
        try (Connection c = db.getConnection();
             PreparedStatement q = c.prepareStatement("SELECT request_hash, review_id FROM idempotency_keys WHERE idem_key = ?")) {
            q.setString(1, key);
            try (ResultSet r = q.executeQuery()) {
                r.next();
                if (!r.getString(1).equals(hash)) {
                    return Map.of("replayed", false, "userErrors", List.of(error(List.of("input", "idempotencyKey"),
                            "This key was already used for a different request.", "KEY_REUSED")));
                }
                return Map.of("review", review(db, r.getInt(2)), "replayed", true, "userErrors", List.of());
            }
        }
    }

    // ---------------------------------------------------------------- optimistic locking

    /** The WHERE clause is the lock: the UPDATE only matches if nobody changed the row since the client read it. */
    static Map<String, Object> renameClub(DataSource db, Map<String, Object> in) throws SQLException {
        String id = (String) in.get("clubId");
        try (Connection c = db.getConnection();
             PreparedStatement u = c.prepareStatement("UPDATE clubs SET name = ?, version = version + 1 WHERE id = ? AND version = ?")) {
            u.setString(1, (String) in.get("name"));
            u.setString(2, id);
            u.setInt(3, (Integer) in.get("expectedVersion"));
            if (u.executeUpdate() == 1) return Map.of("club", club(db, id), "userErrors", List.of());
        }
        Map<String, Object> current = club(db, id);
        if (current == null) return Map.of("userErrors", List.of(error(List.of("input", "clubId"), "No club " + id + ".", "NOT_FOUND")));
        return Map.of("userErrors", List.of(error(List.of("input", "expectedVersion"),
                "The club changed since you read it: version is now " + current.get("version") + ".", "CONFLICT")));
    }

    // ---------------------------------------------------------------- long-running job

    /** Record the job, start the work elsewhere, answer now. */
    static Map<String, Object> importBooks(DataSource db, Map<String, Object> in) throws SQLException {
        @SuppressWarnings("unchecked") List<String> isbns = (List<String>) in.get("isbns");
        String jobId = "job-" + UUID.randomUUID().toString().substring(0, 8);
        update(db, "INSERT INTO jobs VALUES (?, 'QUEUED', ?, 0)", jobId, isbns.size());
        CompletableFuture.runAsync(() -> runImport(db, jobId, isbns), JOBS);
        return Map.of("job", job(db, jobId));
    }

    static void runImport(DataSource db, String jobId, List<String> isbns) {
        try {
            update(db, "UPDATE jobs SET status = 'RUNNING' WHERE id = ?", jobId);
            for (String isbn : isbns) {
                Thread.sleep(100);   // stands in for a call to a slow catalogue service
                if (isbn.matches("97[89]\\d{10}")) {
                    update(db, "INSERT INTO books VALUES (?, ?)", "isbn-" + isbn, isbn);
                    update(db, "UPDATE jobs SET imported = imported + 1 WHERE id = ?", jobId);
                }
            }
            update(db, "UPDATE jobs SET status = 'DONE' WHERE id = ?", jobId);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
