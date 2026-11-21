import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 8.5: one schema, ten requests, errors at every depth.
 *   bin/course java Depths.java         prints every case's response (write predictions.json FIRST)
 *   bin/course java Depths.java E3      prints one
 */
public class Depths {
    static final String SDL = """
        type Query {
          hello: String!
          club(id: ID!): Club
        }
        type Club {
          id: ID!
          name: String!
          owner: User!
          members: [User!]!
          currentBook: Book
          reviews: [Review]
        }
        type User { id: ID! name: String! }
        type Book { title: String! rating: Float }
        type Review { id: ID! body: String! }
        """;

    /** Every case's query. Rx: request errors. Ex: execution errors. Don't edit these. */
    public static final Map<String, String> CASES = new LinkedHashMap<>();
    static {
        CASES.put("R1", "{ club(id: \"c1\") { name ");
        CASES.put("R2", "{ club(id: \"c1\") { name founder } }");
        CASES.put("R3", "query Club($id: ID!) { club(id: $id) { name } }");
        CASES.put("E1", "{ hello club(id: \"c1\") { name } }");
        CASES.put("E2", "{ club(id: \"c1\") { name owner { name } } }");
        CASES.put("E3", "{ club(id: \"c1\") { name members { id name } } }");
        CASES.put("E4", "{ club(id: \"c2\") { name currentBook { title rating } } }");
        CASES.put("E5", "{ club(id: \"c1\") { name currentBook { title rating } } }");
        CASES.put("E6", "{ a: club(id: \"c1\") { owner { name } } b: club(id: \"c2\") { name } }");
        CASES.put("E7", "{ club(id: \"c1\") { reviews { id body } } }");
    }

    record Club(String id, String name, List<String> memberIds, String bookId, List<String> reviewIds) {}
    record User(String id, String name) {}
    record Book(String id, String title, Double rating) {}
    record Review(String id, String body) {}

    static final Map<String, Club> CLUBS = Map.of(
        "c1", new Club("c1", "The Midnight Readers", List.of("u1", "u2", "u3"), "b1", List.of("r1", "r2", "r3")),
        "c2", new Club("c2", "Slow Burn Mysteries", List.of("u2"), "b2", List.of()));
    static final Map<String, User> USERS = Map.of("u1", new User("u1", "Kemi"), "u2", new User("u2", "Asha"), "u3", new User("u3", "Jonas"));
    static final Map<String, Book> BOOKS = Map.of("b1", new Book("b1", "Piranesi", 4.2), "b2", new Book("b2", "The Thursday Murder Club", 3.9));
    static final Map<String, Review> REVIEWS = Map.of("r1", new Review("r1", "Loved it"), "r2", new Review("r2", "Hmm"), "r3", new Review("r3", "Again!"));

    /** The throws, one per depth. The README says which field throws for which object. */
    public static GraphQL graphQL() {
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("hello", e -> { throw new IllegalStateException("greeting service down"); })   // E1: a root field
                .dataFetcher("club", e -> CLUBS.get(e.<String>getArgument("id"))))
            .type("Club", t -> t
                .dataFetcher("owner", e -> { throw new IllegalStateException("users service down"); })       // E2, E6: an object field
                .dataFetcher("members", e -> e.<Club>getSource().memberIds().stream().map(USERS::get).toList())
                .dataFetcher("currentBook", e -> BOOKS.get(e.<Club>getSource().bookId()))
                .dataFetcher("reviews", e -> e.<Club>getSource().reviewIds().stream().map(REVIEWS::get).toList()))
            .type("User", t -> t.dataFetcher("name", e -> {                                                // E3: inside a list item
                User u = e.getSource();
                if (u.id().equals("u3")) throw new IllegalStateException("profile u3 unreadable");
                return u.name();
            }))
            .type("Book", t -> t
                .dataFetcher("title", e -> {                                                               // E4: non-null leaf
                    Book b = e.getSource();
                    if (b.id().equals("b2")) throw new IllegalStateException("title b2 missing");
                    return b.title();
                })
                .dataFetcher("rating", e -> {                                                              // E5: nullable leaf
                    Book b = e.getSource();
                    if (b.id().equals("b1")) throw new IllegalStateException("ratings service down");
                    return b.rating();
                }))
            .type("Review", t -> t.dataFetcher("body", e -> {                                              // E7: list of nullable items
                Review r = e.getSource();
                if (r.id().equals("r2")) throw new IllegalStateException("review r2 corrupt");
                return r.body();
            }))
            .build();
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(SDL), wiring)).build();
    }

    public static Map<String, Object> run(String name) {
        return graphQL().execute(ExecutionInput.newExecutionInput(CASES.get(name)).build()).toSpecification();
    }

    public static void main(String[] args) {
        var json = JsonMapper.builder().build();
        for (String name : CASES.keySet()) {
            if (args.length > 0 && !name.equals(args[0])) continue;
            System.out.println(name + "  " + CASES.get(name));
            System.out.println("    " + json.writeValueAsString(run(name)));
        }
    }
}
