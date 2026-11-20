import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactories;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 3.4 harness. Do not edit. Runs one scenario and prints the response.
 *
 *   java -cp "$(lib)" Nullability.java scenarios/03.json            the scenario's own schema
 *   java -cp "$(lib)" Nullability.java scenarios/bad-day.json fix.graphqls   the same failures, your schema
 *
 * A scenario is {"title", "sdl", "query", "throw": [...], "returnNull": [...]}. Rules look like
 * "Book.averageRating" (every Book) or "User.name@u2" (only the User whose id is u2).
 */
public class Nullability {

    static final JsonMapper JSON = JsonMapper.builder().build();

    /** The same data for every scenario: two clubs, plain maps. */
    static final Map<String, Object> U1 = Map.of("id", "u1", "name", "Asha Rao");
    static final Map<String, Object> U2 = Map.of("id", "u2", "name", "Ben Okafor");
    static final Map<String, Object> U3 = Map.of("id", "u3", "name", "Chloe Martin");
    static final Map<String, Object> U11 = Map.of("id", "u11", "name", "Kemi Adeyemi");
    static final Map<String, Object> B1 = Map.of("id", "b1", "title", "The Left Hand of Darkness", "averageRating", 4.2,
            "reviews", List.of(Map.of("id", "r5", "rating", 4, "author", U3), Map.of("id", "r4", "rating", 3, "author", U2)));
    static final Map<String, Map<String, Object>> CLUBS = Map.of(
            "c1", Map.of("id", "c1", "name", "The Midnight Readers", "description", "Speculative fiction.",
                    "owner", U11, "members", List.of(U11, U1, U2), "currentBook", B1),
            "c2", Map.of("id", "c2", "name", "Slow Burn Mysteries", "description", "Whodunits.",
                    "owner", U3, "members", List.of(U3), "currentBook", B1));

    static boolean matches(List<String> rules, String type, String field, Object source) {
        Object id = source instanceof Map<?, ?> m ? m.get("id") : null;
        return rules.contains(type + "." + field) || rules.contains(type + "." + field + "@" + id);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Map<String, Object> scenario = JSON.readValue(Files.readString(Path.of(args[0])), Map.class);
        String sdl = args.length > 1 ? Files.readString(Path.of(args[1])) : (String) scenario.get("sdl");
        List<String> throwAt = (List<String>) scenario.getOrDefault("throw", List.of());
        List<String> nullAt = (List<String>) scenario.getOrDefault("returnNull", List.of());

        DataFetcher<Object> fetcher = env -> {
            String type = ((GraphQLObjectType) env.getParentType()).getName();
            String field = env.getField().getName();
            Object source = env.getSource();
            if (matches(throwAt, type, field, source)) throw new IllegalStateException(type + "." + field + " failed");
            if (matches(nullAt, type, field, source)) return null;
            if (type.equals("Query")) {
                return switch (field) {
                    case "club" -> CLUBS.get(env.<String>getArgument("id"));
                    case "clubs" -> List.of(CLUBS.get("c1"), CLUBS.get("c2"));
                    default -> null;
                };
            }
            return ((Map<String, Object>) source).get(field);
        };
        var schema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl),
                RuntimeWiring.newRuntimeWiring()
                        .codeRegistry(GraphQLCodeRegistry.newCodeRegistry().defaultDataFetcher(DataFetcherFactories.useDataFetcher(fetcher)))
                        .build());
        var result = GraphQL.newGraphQL(schema).build().execute(ExecutionInput.newExecutionInput((String) scenario.get("query")).build());
        System.out.println(JSON.writeValueAsString(result.toSpecification()));
    }
}
