// Two ordinary GraphQL services (no federation, no knowledge of each other) for lab 15.2's gateway:
//   java PlainService.java catalog 4101     books by id or in batches, titles, authors, pages
//   java PlainService.java reviews 4102     reviews that point at books by bookId only
// POST /graphql as usual. GET /_stats {"service", "requests", "queries": [last 20 documents]}; POST /_reset.
// JDK HttpServer, graphql-java and Jackson (bin/course java's class path).
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.json.JsonMapper;

public class PlainService {
  static final JsonMapper JSON = JsonMapper.builder().build();
  static final AtomicInteger requests = new AtomicInteger();
  static final List<String> queries = new CopyOnWriteArrayList<>();

  record Book(String id, String title, List<String> authors, int pages) {}
  record Review(String id, String bookId, String authorId, int rating, String body) {}

  static final Map<String, Book> BOOKS = new LinkedHashMap<>();
  static final List<Review> REVIEWS = new ArrayList<>();
  static {
    for (Object[] b : new Object[][] {
        {"b1", "The Left Hand of Darkness", "Ursula K. Le Guin", 304}, {"b2", "Piranesi", "Susanna Clarke", 272},
        {"b3", "The Remains of the Day", "Kazuo Ishiguro", 245}, {"b4", "The Name of the Rose", "Umberto Eco", 536},
        {"b5", "Good Omens", "Terry Pratchett|Neil Gaiman", 432}, {"b6", "Project Hail Mary", "Andy Weir", 496},
        {"b7", "The Dispossessed", "Ursula K. Le Guin", 387}, {"b8", "Station Eleven", "Emily St. John Mandel", 333},
        {"b9", "The Seven Moons of Maali Almeida", "Shehan Karunatilaka", 400}, {"b10", "Klara and the Sun", "Kazuo Ishiguro", 303}})
      BOOKS.put((String) b[0], new Book((String) b[0], (String) b[1], List.of(((String) b[2]).split("\\|")), (Integer) b[3]));
    String[][] r = {
        {"r1", "b1", "u1", "5", "Changed how I think about gender in fiction."}, {"r2", "b1", "u2", "4", "Slow first half, devastating second."},
        {"r3", "b1", "u3", "5", "The ice crossing chapter. That's it, that's the review."}, {"r4", "b1", "u10", "3", "Beautiful but I wanted more physics."},
        {"r5", "b1", "u8", "4", "Estraven deserved better."}, {"r6", "b2", "u1", "5", "A house with infinite halls and one perfect narrator."},
        {"r7", "b2", "u6", "4", "Listened twice in a row."}, {"r8", "b4", "u7", "5", "Labyrinthine in every sense."},
        {"r9", "b4", "u12", "4", "Read every Latin footnote. Worth it."}, {"r10", "b6", "u10", "5", "Finally, physics."},
        {"r11", "b6", "u4", "4", "Rocky is the best character of the decade."}, {"r12", "b9", "u5", "5", "Funny and furious."},
        {"r13", "b9", "u3", "4", "The afterlife as bureaucracy, brilliant."}, {"r14", "b3", "u9", "5", "Quietly crushing."},
        {"r15", "b8", "u1", "4", "Survival is insufficient."}};
    for (String[] x : r) REVIEWS.add(new Review(x[0], x[1], x[2], Integer.parseInt(x[3]), x[4]));
  }

  static final String CATALOG = """
      type Query {
        book(id: ID!): Book
        "In the order asked; null for an unknown id. The batch endpoint a gateway should use."
        books(ids: [ID!]!): [Book]
        allBooks: [Book!]!
      }
      type Book { id: ID! title: String! authors: [String!]! pages: Int! }
      """;
  static final String REVIEWS_SDL = """
      type Query {
        review(id: ID!): Review
        "Newest first."
        latestReviews(first: Int = 5): [Review!]!
        reviewsFor(bookId: ID!): [Review!]!
      }
      "A review knows its book's id, nothing else about it."
      type Review { id: ID! bookId: ID! authorId: ID! rating: Int! body: String! }
      """;

  public static void main(String[] args) throws IOException {
    String service = args.length > 0 ? args[0] : "catalog";
    int port = args.length > 1 ? Integer.parseInt(args[1]) : 4101;
    GraphQL graphQL = GraphQL.newGraphQL(service.equals("catalog") ? catalog() : reviews()).build();
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 64);
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    server.createContext("/graphql", ex -> {
      if (!ex.getRequestMethod().equals("POST")) { send(ex, 405, Map.of("error", "POST only")); return; }
      Map<String, Object> body = JSON.readValue(ex.getRequestBody().readAllBytes(), Map.class);
      String query = (String) body.get("query");
      requests.incrementAndGet();
      queries.add(query.replaceAll("\\s+", " ").trim());
      while (queries.size() > 20) queries.remove(0);
      @SuppressWarnings("unchecked") Map<String, Object> vars = (Map<String, Object>) body.getOrDefault("variables", Map.of());
      var result = graphQL.execute(ExecutionInput.newExecutionInput(query).variables(vars == null ? Map.of() : vars)
          .operationName((String) body.get("operationName")).build());
      send(ex, 200, result.toSpecification());
    });
    server.createContext("/_stats", ex -> send(ex, 200, Map.of("service", service, "requests", requests.get(), "queries", List.copyOf(queries))));
    server.createContext("/_reset", ex -> { requests.set(0); queries.clear(); send(ex, 200, Map.of("ok", true)); });
    server.start();
    System.out.println(service + " listening on " + port);
  }

  static graphql.schema.GraphQLSchema catalog() {
    DataFetcher<Object> book = env -> BOOKS.get(env.<String>getArgument("id"));
    DataFetcher<Object> books = env -> env.<List<String>>getArgument("ids").stream().map(BOOKS::get).toList();
    return build(CATALOG, RuntimeWiring.newRuntimeWiring().type("Query", t -> t.dataFetcher("book", book)
        .dataFetcher("books", books).dataFetcher("allBooks", env -> List.copyOf(BOOKS.values()))).build());
  }

  static graphql.schema.GraphQLSchema reviews() {
    return build(REVIEWS_SDL, RuntimeWiring.newRuntimeWiring().type("Query", t -> t
        .dataFetcher("review", env -> REVIEWS.stream().filter(r -> r.id().equals(env.getArgument("id"))).findFirst().orElse(null))
        .dataFetcher("latestReviews", env -> REVIEWS.reversed().stream().limit(Math.max(0, env.<Integer>getArgument("first"))).toList())
        .dataFetcher("reviewsFor", env -> REVIEWS.stream().filter(r -> r.bookId().equals(env.getArgument("bookId"))).toList())).build());
  }

  static graphql.schema.GraphQLSchema build(String sdl, RuntimeWiring wiring) {
    return new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), wiring);
  }

  static void send(HttpExchange ex, int status, Object body) throws IOException {
    byte[] bytes = JSON.writeValueAsBytes(body);
    ex.getResponseHeaders().set("Content-Type", "application/json");
    ex.sendResponseHeaders(status, bytes.length);
    try (var out = ex.getResponseBody()) { out.write(bytes); }
  }
}
