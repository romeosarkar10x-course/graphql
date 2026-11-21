import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import tools.jackson.databind.json.JsonMapper;

/**
 * Generates game 8b's six incidents by running each one for real on graphql-java 26.1: the responses
 * and log lines in incidents/*.md are this program's output, not hand-written.
 *   java -cp <lib> Incidents.java <out dir>
 */
public class Incidents {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final StringBuilder LOG = new StringBuilder();
    static final long START = System.nanoTime();

    static final String SDL = """
        type Query { club(id: ID!): Club  book(id: ID!): Book }
        type Mutation { rateBook(bookId: ID!, rating: Int!): Book! }
        type Club { id: ID! name: String! owner: User! members: [User!]! meetups: [Meetup!]! }
        type User { id: ID! name: String! handle: String! }
        type Meetup { id: ID! title: String! startsAt: String! }
        type Book { id: ID! title: String! reviews: [Review!]! }
        type Review { id: ID! rating: Int! }
        """;

    record User(String id, String name, String handle) {}
    record Meetup(String id, String title, String date) {}
    static final Map<String, User> USERS = new LinkedHashMap<>();
    static {
        USERS.put("u11", new User("u11", "Kemi Adeyemi", "kemia")); USERS.put("u1", new User("u1", "Asha Rao", "asha"));
        USERS.put("u2", new User("u2", "Ben Okafor", "benok")); USERS.put("u3", new User("u3", "Chloe Martin", "chloem"));
        USERS.put("u4", new User("u4", "Dev Patel", null));     // imported from the old system without a handle
        USERS.put("u6", new User("u6", "Farid Haddad", "farid"));
    }

    /** Timestamps are 2026-11-19T21:14:00Z plus the real time elapsed in this run. */
    static void log(String level, String logger, String msg) {
        var at = java.time.Instant.parse("2026-11-19T21:14:00Z").plusNanos(System.nanoTime() - START);
        LOG.append(String.format("%s %-5s [%s] %s%n", java.time.format.DateTimeFormatter.ISO_INSTANT.format(at.truncatedTo(java.time.temporal.ChronoUnit.MILLIS)), level, logger, msg));
    }

    static GraphQL graphQL(boolean ownerReturnsId, boolean slowReviews) {
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("club", e -> Map.of("id", "c1", "name", "The Midnight Readers"))
                .dataFetcher("book", e -> Map.of("id", "b1", "title", "The Left Hand of Darkness")))
            .type("Mutation", t -> t.dataFetcher("rateBook", e -> Map.of("id", "b1", "title", "The Left Hand of Darkness")))
            .type("Club", t -> t
                .dataFetcher("owner", e -> ownerReturnsId ? "u11" : USERS.get("u11"))
                .dataFetcher("members", e -> List.copyOf(USERS.values()))
                .dataFetcher("meetups", e -> List.of(new Meetup("m1", "October: The Dispossessed", "2026-10-28T19:00"),
                    new Meetup("m2", "Winter party", "2026-12-31T20:00"), new Meetup("m3", "January: Piranesi", "2027-01-27T19:00"))))
            .type("Meetup", t -> t.dataFetcher("startsAt", e -> {
                Meetup m = e.getSource();
                return LocalDate.parse(m.date().substring(0, 10)).atTime(java.time.LocalTime.parse(m.date().substring(11))).toString() + ":00Z";
            }))
            .type("Book", t -> t.dataFetcher("reviews", e -> {
                if (slowReviews) {
                    long t0 = System.nanoTime();
                    log("DEBUG", "ReviewRepository", "select r.* from review r where r.book_id = ?");
                    Thread.sleep(900);   // stands in for a slow query
                    log("DEBUG", "ReviewRepository", "reviews of b1 fetched in " + (System.nanoTime() - t0) / 1_000_000 + " ms");
                }
                return List.of(Map.of("id", "r1", "rating", 5));
            }))
            .build();
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(SDL), wiring))
            .defaultDataFetcherExceptionHandler(p -> {
                String errorId = UUID.nameUUIDFromBytes(p.getPath().toString().getBytes()).toString().substring(0, 8);
                var ex = p.getException();
                var frames = Arrays.stream(ex.getStackTrace()).limit(3).map(f -> "\n        at " + f).reduce("", String::concat);
                log("ERROR", "ErrorResolver", "errorId=" + errorId + " path=" + p.getPath() + "\n    " + ex + frames);
                return CompletableFuture.completedFuture(DataFetcherExceptionHandlerResult.newResult(GraphqlErrorBuilder.newError(p.getDataFetchingEnvironment())
                    .message("Internal error").extensions(Map.of("code", "INTERNAL", "errorId", errorId)).build()).build());
            }).build();
    }

    static String run(GraphQL g, String query, Map<String, Object> vars, Integer budgetMs) {
        var input = ExecutionInput.newExecutionInput(query).variables(vars).build();
        log("INFO ", "GraphQlHttpHandler", "POST /graphql operation=" + (query.startsWith("query ") || query.startsWith("mutation ") ? query.split("[ ({]")[1] : "(anonymous)"));
        var timer = budgetMs == null ? null : Executors.newSingleThreadScheduledExecutor().schedule(input::cancel, budgetMs, TimeUnit.MILLISECONDS);
        var result = g.execute(input).toSpecification();
        if (timer != null) timer.cancel(false);
        if (result.containsKey("errors")) {
            @SuppressWarnings("unchecked") var errors = (List<Map<String, Object>>) result.get("errors");
            for (var e : errors) if (!"INTERNAL".equals(((Map<?, ?>) e.getOrDefault("extensions", Map.of())).get("code")))
                log("WARN ", "GraphQlHttpHandler", "error " + ((Map<?, ?>) e.getOrDefault("extensions", Map.of())).get("classification") + ": " + e.get("message"));
        }
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    static void incident(Path dir, int n, String ticket, String query, Map<String, Object> vars, String response, String noise) throws Exception {
        String md = "# Incident " + n + "\n\n**Ticket:** " + ticket + "\n\n**Request**\n\n```graphql\n" + query + "\n```\n"
            + (vars.isEmpty() ? "" : "\nVariables: `" + JSON.writeValueAsString(vars) + "`\n")
            + "\n**Response**\n\n```json\n" + response + "\n```\n\n**Server log (excerpt)**\n\n```\n" + noise + LOG + "```\n";
        Files.writeString(dir.resolve("incident-" + n + ".md"), md);
        LOG.setLength(0);
    }

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[0]);
        Files.createDirectories(dir);
        String noise = "2026-11-19T21:13:58.120Z INFO  [HikariPool] shelfie - Pool stats (total=10, active=1, idle=9, waiting=0)\n";

        var q1 = "query ClubPage {\n  club(id: \"c1\") {\n    name\n    members { name handle }\n  }\n}";
        incident(dir, 1, "\"The Midnight Readers page is completely blank since this morning's data import. Other clubs are fine.\"",
            q1, Map.of(), run(graphQL(false, false), q1, Map.of(), null), noise);

        var q2 = "query ClubMeetups {\n  club(id: \"c1\") {\n    name\n    meetups { title startsAt }\n  }\n}";
        // A second, unrelated failure earlier in the log, so the errorId matters.
        log("ERROR", "ErrorResolver", "errorId=5be13d2a path=/book/coverUrl\n    java.lang.IllegalStateException: S3 GetObject failed: connect timed out\n        at dev.shelfie.boot.ErrorController.coverUrl(ErrorController.java:61)");
        var wiring2 = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t.dataFetcher("club", e -> Map.of("id", "c1", "name", "The Midnight Readers")))
            .type("Club", t -> t.dataFetcher("meetups", e -> List.of(new Meetup("m1", "October: The Dispossessed", "2026-10-28T19:00"),
                new Meetup("m2", "Winter party", "2026-12-31T20:00"), new Meetup("m3", "Book swap", "2026-11-31T18:30"))))
            .type("Meetup", t -> t.dataFetcher("startsAt", e -> {
                Meetup m = e.getSource();
                return LocalDate.parse(m.date().substring(0, 10)).atTime(java.time.LocalTime.parse(m.date().substring(11))).toString() + ":00Z";
            })).build();
        var base = graphQL(false, false);
        var schema2 = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(SDL), wiring2);
        var g2b = base.transform(b -> b.schema(schema2));
        incident(dir, 2, "\"Club c1's meetup list shows an error, and support only has 'Internal error'.\"", q2, Map.of(), run(g2b, q2, Map.of(), null), noise);

        var q3 = "query ClubHeader {\n  club(id: \"c1\") {\n    name\n    memberCount\n  }\n}";
        incident(dir, 3, "\"After Tuesday's deploy the iOS app (version 4.2, released in June) shows 'Something went wrong' on every club. Android 5.0 works.\"",
            q3, Map.of(), run(graphQL(false, false), q3, Map.of(), null), noise);

        var q4 = "mutation RateBook($bookId: ID!, $rating: Int!) {\n  rateBook(bookId: $bookId, rating: $rating) { id }\n}";
        Map<String, Object> v4 = Map.of("bookId", "b1", "rating", "5");
        incident(dir, 4, "\"Rating a book from the new web form never works. The old form did.\"", q4, v4, run(graphQL(false, false), q4, v4, null), noise);

        var q5 = "query BookPage {\n  book(id: \"b1\") {\n    title\n    reviews { rating }\n  }\n}";
        log("INFO ", "Budget", "execution budget 500 ms (shelfie.graphql.timeout)");
        incident(dir, 5, "\"The page for The Left Hand of Darkness never loads. Other books are fine.\"", q5, Map.of(), run(graphQL(false, true), q5, Map.of(), 500), noise);

        var q6 = "query ClubOwner {\n  club(id: \"c1\") {\n    name\n    owner { name handle }\n  }\n}";
        incident(dir, 6, "\"Since the 'faster owner lookup' change, every club page is blank.\"", q6, Map.of(), run(graphQL(true, false), q6, Map.of(), null), noise);
        System.exit(0);   // the budget timer's thread would keep the JVM alive
    }
}
