import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Lab 8.8's schema and data: the ratings service is down and one profile is unreadable. Don't edit. */
public final class Page {
    private Page() {}

    public static final String SDL = """
        type Query { club(id: ID!): Club! }
        type Club { id: ID! name: String! owner: User! members: [User!]! currentBook: Book }
        type User { id: ID! name: String! }
        type Book { id: ID! title: String! rating: Float! }
        """;

    public static final String QUERY = "{ club(id: \"c1\") { id name currentBook { id title rating } owner { id name } members { id name } } }";

    /** How many fetchers ran: HALT should stop early. */
    public static final AtomicInteger FETCHES = new AtomicInteger();

    record Club(String id, String name, String ownerId, List<String> memberIds, String bookId) {}
    record User(String id, String name) {}
    record Book(String id, String title) {}

    static final Map<String, User> USERS = Map.of("u1", new User("u1", "Asha"), "u2", new User("u2", "Ben"), "u3", new User("u3", "Chloe"), "u11", new User("u11", "Kemi"));

    public static GraphQL graphQL() {
        var club = new Club("c1", "The Midnight Readers", "u11", List.of("u1", "u2", "u3"), "b1");
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t.dataFetcher("club", e -> { FETCHES.incrementAndGet(); return club; }))
            .type("Club", t -> t
                .dataFetcher("currentBook", e -> { FETCHES.incrementAndGet(); return new Book("b1", "Piranesi"); })
                .dataFetcher("owner", e -> { FETCHES.incrementAndGet(); return USERS.get("u11"); })
                .dataFetcher("members", e -> { FETCHES.incrementAndGet(); return club.memberIds().stream().map(USERS::get).toList(); }))
            .type("User", t -> t.dataFetcher("name", e -> {
                FETCHES.incrementAndGet();
                User u = e.getSource();
                if (u.id().equals("u3")) throw new IllegalStateException("profile u3 unreadable");
                return u.name();
            }))
            .type("Book", t -> t.dataFetcher("rating", e -> { FETCHES.incrementAndGet(); throw new IllegalStateException("ratings service down"); }))
            .build();
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(SDL), wiring)).build();
    }
}
