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

    /** One line: indent by depth, [i] when inside a list, the key, its arguments, the source type. */
    static void record(DataFetchingEnvironment env) {
        var info = env.getExecutionStepInfo();
        var path = info.getPath();
        int depth = (int) path.toList().stream().filter(s -> s instanceof String).count() - 1;
        var parent = path.getParent();
        String index = parent != null && parent.isListSegment() ? "[" + parent.getSegmentIndex() + "] " : "";
        String args = env.getArguments().isEmpty() ? "" : new TreeMap<>(env.getArguments()).entrySet().stream()
            .map(e -> e.getKey() + ": " + (e.getValue() instanceof String str ? "\"" + str + "\"" : e.getValue()))
            .collect(Collectors.joining(", ", "(", ")"));
        Object source = env.getSource();
        String from = depth == 0 ? "root" : source.getClass().getSimpleName();
        String selects = depth == 0 && !env.getSelectionSet().getImmediateFields().isEmpty()
            ? "  selects " + env.getSelectionSet().getImmediateFields().stream().map(SelectedField::getName).toList() : "";
        LINES.add("  ".repeat(depth) + index + path.getSegmentName() + args + "  <- " + from + selects);
    }

    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("club", env -> Data.club(env.getArgument("id")).orElse(null))
                .dataFetcher("user", env -> Data.user(env.getArgument("id")).orElse(null))
                .dataFetcher("viewer", env -> {
                    String id = env.getGraphQlContext().get("viewer");
                    return id == null ? null : Data.user(id).orElse(null);
                })
                .dataFetcher("booksById", CallTree::booksById))
            .type("Club", t -> t
                // The club travels down as local context, so User.roleInClub knows where it was reached from.
                .dataFetcher("owner", env -> {
                    Data.Club c = env.getSource();
                    return DataFetcherResult.newResult().data(Data.user(c.ownerId()).orElseThrow()).localContext(c).build();
                })
                .dataFetcher("members", env -> {
                    Data.Club c = env.getSource();
                    int first = env.getArgumentOrDefault("first", 10);
                    var users = c.memberIds().stream().limit(Math.max(0, first)).map(id -> Data.user(id).orElseThrow()).toList();
                    return DataFetcherResult.newResult().data(users).localContext(c).build();
                })
                .dataFetcher("memberCount", env -> env.<Data.Club>getSource().memberIds().size()))
            .type("User", t -> t
                .dataFetcher("clubs", env -> Data.clubsOf(env.<Data.User>getSource().id()))
                .dataFetcher("reviews", env -> Data.reviewsBy(env.<Data.User>getSource().id()))
                .dataFetcher("roleInClub", env -> {
                    if (!(env.getLocalContext() instanceof Data.Club c)) return null;
                    return c.ownerId().equals(env.<Data.User>getSource().id()) ? "OWNER" : "MEMBER";
                }))
            .type("Book", t -> t
                .dataFetcher("averageRating", env -> Data.averageRating(env.<Data.Book>getSource().id())))
            .type("Review", t -> t
                .dataFetcher("author", env -> Data.user(env.<Data.Review>getSource().authorId()).orElseThrow())
                .dataFetcher("book", env -> Data.book(env.<Data.Review>getSource().bookId()).orElseThrow()))
            .build();
    }

    /** Partial success: the found books, a null in each gap, and one error per gap pointing at it. */
    static DataFetcherResult<List<Data.Book>> booksById(DataFetchingEnvironment env) {
        List<String> ids = env.getArgument("ids");
        var books = new ArrayList<Data.Book>();
        var result = DataFetcherResult.<List<Data.Book>>newResult();
        for (int i = 0; i < ids.size(); i++) {
            var b = Data.book(ids.get(i));
            books.add(b.orElse(null));
            if (b.isEmpty()) result.error(GraphqlErrorBuilder.newError(env)
                .message("No book with id " + ids.get(i))
                .path(env.getExecutionStepInfo().getPath().segment(i))
                .extensions(Map.of("code", "NOT_FOUND")).build());
        }
        return result.data(books).build();
    }
}
