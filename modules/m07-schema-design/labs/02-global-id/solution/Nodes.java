import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeRuntimeWiring;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Lab 7.2: node(id:) and nodes(ids:) over a registry of types. The harness calls graphQL(). */
final class Nodes {
    private Nodes() {}

    /** One entry per Node type: its Java class, how to load it by key, and how to read its key. */
    record Kind<T>(Class<T> javaType, Function<String, Optional<T>> load, Function<T, String> key) {}

    static final Map<String, Kind<?>> REGISTRY = Map.of(
        "Club", new Kind<>(Data.Club.class, Data::club, Data.Club::id),
        "User", new Kind<>(Data.User.class, Data::user, Data.User::id),
        "Book", new Kind<>(Data.Book.class, Data::book, Data.Book::id),
        "Review", new Kind<>(Data.Review.class, Data::review, Data.Review::id),
        "Meetup", new Kind<>(Data.Meetup.class, Data::meetup, Data.Meetup::id));

    /** The object behind a global id, or null. A known type with an unknown key is null too. */
    static Object load(String opaque) {
        return GlobalId.decode(opaque)
            .flatMap(g -> Optional.ofNullable(REGISTRY.get(g.type())).flatMap(k -> k.load().apply(g.key()).map(o -> (Object) o)))
            .orElse(null);
    }

    /** The GraphQL type name for a Java object from Data. */
    static String typeOf(Object o) {
        return REGISTRY.entrySet().stream().filter(e -> e.getValue().javaType().isInstance(o)).map(Map.Entry::getKey).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    static <T> String globalId(Object o) {
        String type = typeOf(o);
        Kind<T> k = (Kind<T>) REGISTRY.get(type);
        return new GlobalId(type, k.key().apply((T) o)).encode();
    }

    static GraphQL graphQL() throws Exception {
        DataFetcher<String> id = env -> globalId(env.getSource());
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("node", env -> load(env.getArgument("id")))
                .dataFetcher("nodes", env -> env.<List<String>>getArgument("ids").stream().map(Nodes::load).toList())
                .dataFetcher("clubs", env -> Data.CLUBS))
            .type("Node", t -> t.typeResolver(env -> env.getSchema().getObjectType(typeOf(env.getObject()))))
            .type(TypeRuntimeWiring.newTypeWiring("Club").dataFetcher("id", id)
                .dataFetcher("owner", env -> Data.user(env.<Data.Club>getSource().ownerId()).orElseThrow())
                .dataFetcher("currentBook", env -> Data.book(env.<Data.Club>getSource().currentBookId()).orElse(null))
                .dataFetcher("meetups", env -> Data.meetupsOf(env.<Data.Club>getSource().id(), false)))
            .type("User", t -> t.dataFetcher("id", id))
            .type("Book", t -> t.dataFetcher("id", id).dataFetcher("reviews", env -> Data.reviewsOf(env.<Data.Book>getSource().id())))
            .type("Review", t -> t.dataFetcher("id", id).dataFetcher("author", env -> Data.user(env.<Data.Review>getSource().authorId()).orElseThrow()))
            .type("Meetup", t -> t.dataFetcher("id", id))
            .build();
        var sdl = Files.readString(Path.of("nodes.graphqls"));
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), wiring)).build();
    }
}
