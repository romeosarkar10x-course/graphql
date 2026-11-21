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
    /** B1 fix: blocking calls get their own executor; virtual threads make that cheap. */
    static final ExecutorService BLOCKING = Executors.newVirtualThreadPerTaskExecutor();
    static final ThreadLocal<String> VIEWER = new ThreadLocal<>();   // B7: no longer read
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
                // B5 fix: an explicit null overrides the default, and first may exceed the list
                .dataFetcher("books", e -> {
                    Integer first = e.getArgument("first");
                    return Data.BOOKS.stream().limit(first == null ? Data.BOOKS.size() : Math.max(0, first)).toList();
                }))
            .type("Club", t -> t
                // B2 fix: no shared mutable buffer; build the string locally
                .dataFetcher("label", e -> {
                    Data.Club c = e.getSource();
                    return c.name() + " (" + c.memberIds().size() + ")";
                })
                // B6 fix: the owner's id, not the club's; and fail loudly instead of a silent null
                .dataFetcher("owner", e -> Data.user(e.<Data.Club>getSource().ownerId()).orElseThrow())
                .dataFetcher("members", e -> MEMBERS.get(e.<Data.Club>getSource().id()).stream()
                    .map(id -> Data.user(id).orElseThrow()).toList())
                // B4 fix: sort a copy; the store's list is not ours to reorder
                .dataFetcher("membersByName", e -> MEMBERS.get(e.<Data.Club>getSource().id()).stream()
                    .map(id -> Data.user(id).orElseThrow()).sorted(Comparator.comparing(Data.User::name)).toList())
                // B8 fix: return the repository's future; never block a pool thread on work queued to the same pool
                .dataFetcher("currentBook", e -> loadBook(e.<Data.Club>getSource().currentBookId()))
                // B7 fix: request values travel in GraphQLContext, which every thread can see
                .dataFetcher("viewerIsMember", e -> CompletableFuture.supplyAsync(() -> {
                    String viewer = e.getGraphQlContext().get("viewer");
                    return viewer == null ? null : e.<Data.Club>getSource().memberIds().contains(viewer);
                }, POOL)))
            .type("Book", t -> t
                // B1 fix: an explicit executor; the common pool is for short CPU work
                .dataFetcher("averageRating", e -> CompletableFuture.supplyAsync(() -> ratingsService(e.<Data.Book>getSource().id()), BLOCKING))
                .dataFetcher("reviews", e -> Data.reviewsOf(e.<Data.Book>getSource().id())))
            .type("Review", t -> t
                // B3 fix: the source is whatever the parent fetcher returned: a Data.Review record
                .dataFetcher("book", e -> Data.book(e.<Data.Review>getSource().bookId()).orElseThrow()))
            .build();
    }

    static GraphQL graphQL() throws Exception {
        var schema = new SchemaGenerator().makeExecutableSchema(
            new SchemaParser().parse(Files.readString(Path.of("spot.graphqls"))), wiring());
        return GraphQL.newGraphQL(schema).build();
    }

    static ExecutionInput input(String query, String viewer) {
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
