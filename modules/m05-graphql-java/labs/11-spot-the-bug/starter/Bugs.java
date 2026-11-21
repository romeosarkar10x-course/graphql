import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Game 5b: eight fetchers, eight classic mistakes. Each works in a quick manual test; each breaks in
 * production. Find them, write them up in bugs.md, fix them here. The harness calls Bugs.graphQL()
 * and Bugs.input(query, viewer) and reads Bugs.RATING_THREADS; keep those.
 */
public class Bugs {
    /** Thread names that ran the ratings call, for the harness. */
    static final Set<String> RATING_THREADS = ConcurrentHashMap.newKeySet();
    static final ExecutorService POOL = Executors.newFixedThreadPool(2);
    static final ThreadLocal<String> VIEWER = new ThreadLocal<>();
    static final StringBuilder LABEL = new StringBuilder();
    /** The store: members per club, as the database would hand them over. */
    static final Map<String, List<String>> MEMBERS = new ConcurrentHashMap<>();
    static { Data.CLUBS.forEach(c -> MEMBERS.put(c.id(), new ArrayList<>(c.memberIds()))); }

    static Double ratingsService(String bookId) {            // a slow remote call
        RATING_THREADS.add(Thread.currentThread().getName());
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return Data.averageRating(bookId);
    }

    static CompletableFuture<Data.Book> loadBook(String id) {    // an async repository
        return CompletableFuture.supplyAsync(() -> Data.book(id).orElse(null), POOL);
    }

    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("clubs", e -> Data.CLUBS)
                .dataFetcher("club", e -> Data.club(e.getArgument("id")).orElse(null))
                // B5
                .dataFetcher("books", e -> Data.BOOKS.subList(0, (int) e.getArgument("first"))))
            .type("Club", t -> t
                // B2
                .dataFetcher("label", e -> {
                    Data.Club c = e.getSource();
                    LABEL.setLength(0);
                    LABEL.append(c.name()).append(" (").append(c.memberIds().size()).append(")");
                    return LABEL.toString();
                })
                // B6
                .dataFetcher("owner", e -> Data.user(e.<Data.Club>getSource().id()).orElse(null))
                .dataFetcher("members", e -> MEMBERS.get(e.<Data.Club>getSource().id()).stream()
                    .map(id -> Data.user(id).orElseThrow()).toList())
                // B4
                .dataFetcher("membersByName", e -> {
                    List<String> ids = MEMBERS.get(e.<Data.Club>getSource().id());
                    ids.sort(Comparator.comparing(id -> Data.user(id).orElseThrow().name()));
                    return ids.stream().map(id -> Data.user(id).orElseThrow()).toList();
                })
                // B8
                .dataFetcher("currentBook", e -> CompletableFuture.supplyAsync(
                    () -> loadBook(e.<Data.Club>getSource().currentBookId()).join(), POOL))
                // B7
                .dataFetcher("viewerIsMember", e -> CompletableFuture.supplyAsync(() -> {
                    String viewer = VIEWER.get();
                    return viewer == null ? null : e.<Data.Club>getSource().memberIds().contains(viewer);
                }, POOL)))
            .type("Book", t -> t
                // B1
                .dataFetcher("averageRating", e -> CompletableFuture.supplyAsync(() -> ratingsService(e.<Data.Book>getSource().id())))
                .dataFetcher("reviews", e -> Data.reviewsOf(e.<Data.Book>getSource().id())))
            .type("Review", t -> t
                // B3
                .dataFetcher("book", e -> {
                    Map<String, Object> review = e.getSource();
                    return Data.book((String) review.get("bookId")).orElse(null);
                }))
            .build();
    }

    static GraphQL graphQL() throws Exception {
        var schema = new SchemaGenerator().makeExecutableSchema(
            new SchemaParser().parse(Files.readString(Path.of("spot.graphqls"))), wiring());
        return GraphQL.newGraphQL(schema).build();
    }

    static ExecutionInput input(String query, String viewer) {
        VIEWER.set(viewer);
        var b = ExecutionInput.newExecutionInput(query);
        if (viewer != null) b.graphQLContext(Map.of("viewer", viewer));
        return b.build();
    }

    public static void main(String[] args) throws Exception {
        var r = graphQL().execute(input(args[0], args.length > 1 ? args[1] : null));
        System.out.println(r.toSpecification());
        POOL.shutdown();
    }
}
