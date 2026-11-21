import graphql.ExecutionInput;
import graphql.GraphQL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 5.1. Three commands:
 *   run <query.graphql> [variables-json]   print ExecutionResult.toSpecification() as JSON
 *   phases <query.graphql>                 time parse, validate and execute separately
 *   gen <n>                                print a query with n aliased fields
 */
public class Phases {
    static final JsonMapper JSON = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {
        var schema = Shelfie.schema(Path.of("shelfie.graphqls"));
        var graphQL = GraphQL.newGraphQL(schema).build();
        switch (args[0]) {
            case "run" -> {
                Map<String, Object> vars = args.length > 2
                    ? JSON.readValue(args[2], new tools.jackson.core.type.TypeReference<Map<String, Object>>() {}) : Map.of();
                var input = ExecutionInput.newExecutionInput(Files.readString(Path.of(args[1]))).variables(vars).build();
                // TODO: this prints only the data. The spec response map has data, errors and extensions,
                // each present only when it should be. ExecutionResult knows how to build it.
                var data = graphQL.execute(input).getData();
                System.out.println(JSON.writeValueAsString(java.util.Collections.singletonMap("data", data)));
            }
            case "phases" -> phases(schema, Files.readString(Path.of(args[1])));
            case "gen" -> {
                var sb = new StringBuilder("{");
                for (int i = 0; i < Integer.parseInt(args[1]); i++) sb.append(" f").append(i).append(": hello");
                System.out.println(sb.append(" }"));
            }
            default -> throw new IllegalArgumentException("run | phases | gen");
        }
    }

    static void phases(graphql.schema.GraphQLSchema schema, String query) {
        // TODO: parse with graphql.parser.Parser, validate with graphql.validation.Validator, then execute
        // WITHOUT parsing and validating again, timing each step. Print:
        //   parse <ms> ms / validate <ms> ms / execute <ms> ms / validation-errors <n> / fields <n>
        throw new UnsupportedOperationException("TODO");
    }
}
