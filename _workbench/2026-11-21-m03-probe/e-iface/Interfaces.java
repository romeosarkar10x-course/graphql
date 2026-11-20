import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.TypeResolver;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Lab 3.6: three interfaces, one of which implements another, and the TypeResolvers that pick the
 * concrete type for each value.
 *
 *   java -cp "$(lib)" Interfaces.java query.graphql ['{...}']
 */
public class Interfaces {

    /** One resolver for all three interfaces: the Java class decides the GraphQL type. */
    static final TypeResolver BY_RECORD = env -> {
        String name = switch (env.getObject()) {
            case Data.Book b -> "Book";
            case Data.Club c -> "Club";
            case Data.User u -> "User";
            case Data.Review r -> "Review";
            default -> throw new IllegalStateException("No GraphQL type for " + env.getObject().getClass());
        };
        return env.getSchema().getObjectType(name);
    };

    /** Ids are globally unique in the seed: b1, c1, u1, r1. */
    static Optional<Object> node(String id) {
        return Stream.<Optional<?>>of(Data.book(id), Data.club(id), Data.user(id),
                        Data.REVIEWS.stream().filter(r -> r.id().equals(id)).findFirst())
                .flatMap(Optional::stream).map(o -> (Object) o).findFirst();
    }

    static String createdAt(Object o) {
        return o instanceof Data.Club c ? c.createdAt() : ((Data.Review) o).createdAt();
    }

    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type("Node", t -> t.typeResolver(BY_RECORD))
                .type("Timestamped", t -> t.typeResolver(BY_RECORD))
                .type("Reviewable", t -> t.typeResolver(BY_RECORD))
                .type("Query", t -> t
                        .dataFetcher("node", env -> node(env.getArgument("id")).orElse(null))
                        .dataFetcher("nodes", env -> env.<List<String>>getArgument("ids").stream().map(id -> node(id).orElse(null)).toList())
                        .dataFetcher("reviewables", env -> Data.BOOKS.stream().filter(b -> Data.averageRating(b.id()) != null)
                                .sorted(Comparator.comparing((Data.Book b) -> Data.averageRating(b.id())).reversed()).toList())
                        .dataFetcher("recent", env -> Stream.concat(Data.CLUBS.stream(), Data.REVIEWS.stream())
                                .map(o -> (Object) o)
                                .sorted(Comparator.comparing(Interfaces::createdAt).reversed())
                                .limit(Math.max(0, env.<Integer>getArgument("first"))).toList()))
                .type("Book", t -> t
                        .dataFetcher("averageRating", env -> Data.averageRating(env.<Data.Book>getSource().id()))
                        .dataFetcher("reviews", env -> Data.reviewsOf(env.<Data.Book>getSource().id()).stream()
                                .limit(Math.max(0, env.<Integer>getArgument("last"))).toList()))
                .type("Review", t -> t
                        .dataFetcher("author", env -> Data.user(env.<Data.Review>getSource().authorId()).orElseThrow()))
                .build();
    }

    public static void main(String[] args) throws Exception {
        var schema = new SchemaGenerator().makeExecutableSchema(
                new SchemaParser().parse(Files.readString(Path.of("schema.graphqls"))), wiring());
        Map<String, Object> vars = args.length > 1 ? Json.read(args[1]) : Map.of();
        var result = GraphQL.newGraphQL(schema).build()
                .execute(ExecutionInput.newExecutionInput(Files.readString(Path.of(args[0]))).variables(vars).build());
        System.out.println(Json.of(result.toSpecification()));
    }
}
