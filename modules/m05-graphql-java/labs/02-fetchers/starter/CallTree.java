import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.SelectedField;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 5.2: every fetcher, and what it is handed.
 *   tree <query.graphql>             print one line per fetcher call, as a tree
 *   run <query.graphql> [viewerId]   print the spec response map; viewerId goes into GraphQLContext
 */
public class CallTree {
    static final List<String> LINES = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        var types = new TypeDefinitionRegistry();
        types.merge(new SchemaParser().parse(Files.readString(Path.of("shelfie.graphqls"))));
        types.merge(new SchemaParser().parse(Files.readString(Path.of("lab.graphqls"))));
        var schema = new SchemaGenerator().makeExecutableSchema(types, wiring());
        var builder = GraphQL.newGraphQL(schema);
        if (args[0].equals("tree")) builder.instrumentation(new Tracing());   // wraps every fetcher, even the default ones
        var input = ExecutionInput.newExecutionInput(Files.readString(Path.of(args[1])));
        if (args.length > 2) input.graphQLContext(Map.of("viewer", args[2]));
        var result = builder.build().execute(input.build());
        if (args[0].equals("tree")) LINES.forEach(System.out::println);
        else System.out.println(JsonMapper.builder().build().writeValueAsString(result.toSpecification()));
    }

    /** Lets record() see every fetcher call, including the PropertyDataFetchers nobody wrote. Module 5.5 explains it. */
    static final class Tracing extends SimplePerformantInstrumentation {
        @Override
        public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> df, InstrumentationFieldFetchParameters p, InstrumentationState s) {
            return env -> { record(env); return df.get(env); };
        }
    }

    /**
     * TODO: add one line per fetcher call to LINES. Format (see README):
     *   two spaces per level of depth, "[i] " when the parent is a list item, the response key,
     *   "(name: value, ...)" sorted by name when the field has arguments (strings in quotes),
     *   "  <- " and the source's simple class name ("root" for root fields), and for root fields
     *   "  selects [a, b, c]": the names of the immediate sub-fields.
     * Everything you need hangs off env: getExecutionStepInfo(), getArguments(), getSource(), getSelectionSet().
     */
    static void record(DataFetchingEnvironment env) {
        LINES.add(env.getField().getName());
    }

    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("club", env -> Data.club(env.getArgument("id")).orElse(null))
                .dataFetcher("user", env -> Data.user(env.getArgument("id")).orElse(null))
                // TODO: the viewer's id is in the GraphQLContext under "viewer" (main puts it there)
                .dataFetcher("viewer", env -> null)
                .dataFetcher("booksById", CallTree::booksById))
            .type("Club", t -> t
                // TODO: User.roleInClub needs to know which club a user was reached through. Hand it down.
                .dataFetcher("owner", env -> Data.user(env.<Data.Club>getSource().ownerId()).orElseThrow())
                .dataFetcher("members", env -> {
                    Data.Club c = env.getSource();
                    int first = env.getArgumentOrDefault("first", 10);
                    return c.memberIds().stream().limit(Math.max(0, first)).map(id -> Data.user(id).orElseThrow()).toList();
                })
                .dataFetcher("memberCount", env -> env.<Data.Club>getSource().memberIds().size()))
            .type("User", t -> t
                .dataFetcher("clubs", env -> Data.clubsOf(env.<Data.User>getSource().id()))
                .dataFetcher("reviews", env -> Data.reviewsBy(env.<Data.User>getSource().id()))
                .dataFetcher("roleInClub", env -> null))   // TODO: OWNER, MEMBER or null
            .type("Book", t -> t
                .dataFetcher("averageRating", env -> Data.averageRating(env.<Data.Book>getSource().id())))
            .type("Review", t -> t
                .dataFetcher("author", env -> Data.user(env.<Data.Review>getSource().authorId()).orElseThrow())
                .dataFetcher("book", env -> Data.book(env.<Data.Review>getSource().bookId()).orElseThrow()))
            .build();
    }

    /** TODO: partial success. Today an unknown id is a silent null; the client cannot tell "gone" from "broken". */
    static List<Data.Book> booksById(DataFetchingEnvironment env) {
        List<String> ids = env.getArgument("ids");
        return ids.stream().map(id -> Data.book(id).orElse(null)).toList();
    }
}
