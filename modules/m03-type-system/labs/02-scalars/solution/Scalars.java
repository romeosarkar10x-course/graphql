import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lab 3.2: Java's long meets GraphQL's 32-bit Int.
 *
 *   java -cp "$(lib)" Scalars.java query.graphql ['{"id":"..."}']
 */
public class Scalars {

    /** The analytics store keys rows with 64-bit longs. 2^53 + 1 is the first one a JavaScript number cannot hold. */
    record AnalyticsReview(long id, String bookId, int rating) {}

    static final List<AnalyticsReview> REVIEWS = List.of(
            new AnalyticsReview(42L, "b1", 4),
            new AnalyticsReview(9_007_199_254_740_993L, "b2", 5),
            new AnalyticsReview(9_007_199_254_740_994L, "b6", 3));

    static final long TOTAL_PAGE_VIEWS = 3_000_000_000L;

    /** ID arrives as a String. Malformed means "no such review", not a crash. */
    static Optional<AnalyticsReview> find(String id) {
        long key;
        try {
            key = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return REVIEWS.stream().filter(r -> r.id() == key).findFirst();
    }

    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
                .scalar(ExtendedScalars.GraphQLLong)
                .type("Query", t -> t
                        .dataFetcher("review", env -> find(env.getArgument("id")).orElse(null))
                        .dataFetcher("stats", env -> Map.of()))
                .type("AnalyticsReview", t -> t
                        .dataFetcher("id", env -> Long.toString(env.<AnalyticsReview>getSource().id())))
                .type("Stats", t -> t
                        .dataFetcher("reviewCount", env -> REVIEWS.size())
                        .dataFetcher("totalPageViews", env -> TOTAL_PAGE_VIEWS))
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
