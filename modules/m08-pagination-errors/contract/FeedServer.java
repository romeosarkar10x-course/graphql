import com.sun.net.httpserver.HttpServer;
import graphql.GraphQL;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import tools.jackson.databind.json.JsonMapper;

/**
 * Module 8's feed server (game 8a, mini-project 8): a newest-first book feed over GraphQL, with offset
 * paging and a cursor connection, and a bot that keeps adding (and optionally deleting) books.
 *
 *   bin/course java FeedServer.java [--port 4000] [--books 500] [--every 1000] [--deletes] [--seed 8]
 *
 * --every N: the bot adds a book every N ms (0: no bot). --deletes: it also deletes a random older book.
 * GET /stats gives {"requests": n, "added": n, "deleted": n} (GraphQL requests); POST /stats does too, then zeroes them.
 */
public class FeedServer {
    static final String SDL = """
        type Query {
          "Newest first. offset counts from the top as it is now."
          feedPage(offset: Int!, limit: Int!): [Book!]!
          "Newest first, keyset paging on id. first is 1 to 20."
          feed(first: Int!, after: String): BookConnection!
          "The books added above this cursor, oldest of them first, at most 20."
          newerThan(cursor: String!): [Book!]!
          "Every book, newest first: for tests."
          allIds: [ID!]!
        }
        type BookConnection { edges: [BookEdge!]! pageInfo: PageInfo! }
        type BookEdge { cursor: String! node: Book! }
        type PageInfo { hasNextPage: Boolean! endCursor: String startCursor: String }
        type Book { id: ID! title: String! author: String! }
        """;
    static final String[] ADJ = {"Silent", "Hidden", "Burning", "Last", "Glass", "Winter", "Paper", "Iron", "Velvet", "Hollow"};
    static final String[] NOUN = {"Garden", "River", "House", "Clock", "Library", "Harbour", "Orchard", "Mirror", "Tower", "Island", "Letter"};
    static final String[] AUTHOR = {"Ada Okafor", "Kemi Lindqvist", "Jonas Tanaka", "Mei Moreau", "Ravi Iyer", "Sofia Novak"};

    record Book(long id, String title, String author) {}

    static final NavigableMap<Long, Book> BOOKS = new ConcurrentSkipListMap<>();
    static final AtomicLong NEXT = new AtomicLong(1);
    static final AtomicInteger REQUESTS = new AtomicInteger(), ADDED = new AtomicInteger(), DELETED = new AtomicInteger();

    static Book make(long id) { return new Book(id, ADJ[(int) (id * 7 % ADJ.length)] + " " + NOUN[(int) (id * 3 % NOUN.length)], AUTHOR[(int) (id % AUTHOR.length)]); }
    static void add() { long id = NEXT.getAndIncrement(); BOOKS.put(id, make(id)); }

    static String cursor(long id) { return Base64.getUrlEncoder().withoutPadding().encodeToString(("feed:" + id).getBytes(StandardCharsets.UTF_8)); }
    static Long decode(String c) {
        try {
            var s = new String(Base64.getUrlDecoder().decode(c), StandardCharsets.UTF_8);
            return s.startsWith("feed:") ? Long.parseLong(s.substring(5)) : null;
        } catch (IllegalArgumentException e) { return null; }
    }

    public static void main(String[] args) throws Exception {
        int port = 4000, books = 500; long every = 1000, seed = 8; boolean deletes = false;
        for (int i = 0; i < args.length; i++) switch (args[i]) {
            case "--port" -> port = Integer.parseInt(args[++i]);
            case "--books" -> books = Integer.parseInt(args[++i]);
            case "--every" -> every = Long.parseLong(args[++i]);
            case "--seed" -> seed = Long.parseLong(args[++i]);
            case "--deletes" -> deletes = true;
            default -> { System.err.println("unknown option " + args[i]); System.exit(2); }
        }
        for (int i = 0; i < books; i++) add();
        var json = JsonMapper.builder().build();
        var graphQL = GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(SDL), wiring())).build();

        if (every > 0) {
            var random = new Random(seed);
            boolean del = deletes;
            Executors.newSingleThreadScheduledExecutor(r -> { var t = new Thread(r, "bot"); t.setDaemon(true); return t; })
                .scheduleAtFixedRate(() -> {
                    add(); ADDED.incrementAndGet();
                    if (del && BOOKS.size() > 10) {
                        // a random book that is not among the 5 newest
                        var ids = new ArrayList<>(BOOKS.headMap(BOOKS.lastKey() - 5).keySet());
                        if (!ids.isEmpty() && BOOKS.remove(ids.get(random.nextInt(ids.size()))) != null) DELETED.incrementAndGet();
                    }
                }, every, every, TimeUnit.MILLISECONDS);
        }

        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/graphql", ex -> {
            try (ex) {
                REQUESTS.incrementAndGet();
                @SuppressWarnings("unchecked") Map<String, Object> req = json.readValue(ex.getRequestBody(), Map.class);
                @SuppressWarnings("unchecked") var vars = (Map<String, Object>) req.getOrDefault("variables", Map.of());
                var result = graphQL.execute(b -> b.query((String) req.get("query")).variables(vars == null ? Map.of() : vars));
                byte[] out = json.writeValueAsBytes(result.toSpecification());
                ex.getResponseHeaders().set("Content-Type", "application/json");
                ex.sendResponseHeaders(200, out.length);
                ex.getResponseBody().write(out);
            }
        });
        server.createContext("/stats", ex -> {
            try (ex) {
                byte[] out = json.writeValueAsBytes(Map.of("requests", REQUESTS.get(), "added", ADDED.get(), "deleted", DELETED.get()));
                if (ex.getRequestMethod().equals("POST")) { REQUESTS.set(0); ADDED.set(0); DELETED.set(0); }
                ex.getResponseHeaders().set("Content-Type", "application/json");
                ex.sendResponseHeaders(200, out.length);
                ex.getResponseBody().write(out);
            }
        });
        server.start();
        System.out.printf("feed: %d books on http://localhost:%d/graphql, bot every %d ms%s%n", books, port, every, deletes ? " with deletes" : "");
    }

    static Map<String, Object> node(Book b) { return Map.of("id", String.valueOf(b.id()), "title", b.title(), "author", b.author()); }

    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("feedPage", e -> {
                    int offset = Math.max(0, e.<Integer>getArgument("offset")), limit = Math.clamp(e.<Integer>getArgument("limit"), 0, 50);
                    return BOOKS.descendingMap().values().stream().skip(offset).limit(limit).map(FeedServer::node).toList();
                })
                .dataFetcher("feed", e -> {
                    int first = e.getArgument("first");
                    String after = e.getArgument("after");
                    if (first < 1 || first > 20) return DataFetcherResult.newResult()
                        .error(GraphqlErrorBuilder.newError(e).message("first must be 1 to 20").build()).build();
                    Long from = after == null ? Long.MAX_VALUE : decode(after);
                    if (from == null) return DataFetcherResult.newResult()
                        .error(GraphqlErrorBuilder.newError(e).message("Not a feed cursor").build()).build();
                    var rows = BOOKS.headMap(from, false).descendingMap().values().stream().limit(first + 1).toList();
                    var page = rows.subList(0, Math.min(first, rows.size()));
                    var edges = page.stream().map(b -> Map.of("cursor", cursor(b.id()), "node", node(b))).toList();
                    var info = new java.util.HashMap<String, Object>();
                    info.put("hasNextPage", rows.size() > first);
                    info.put("startCursor", page.isEmpty() ? null : cursor(page.getFirst().id()));
                    info.put("endCursor", page.isEmpty() ? null : cursor(page.getLast().id()));
                    return Map.of("edges", edges, "pageInfo", info);
                })
                .dataFetcher("newerThan", e -> {
                    Long top = decode(e.getArgument("cursor"));
                    if (top == null) return List.of();
                    return BOOKS.tailMap(top, false).values().stream().limit(20).map(FeedServer::node).toList();
                })
                .dataFetcher("allIds", e -> BOOKS.descendingKeySet().stream().map(String::valueOf).toList()))
            .build();
    }
}
