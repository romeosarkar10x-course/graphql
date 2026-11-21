import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.parser.ParserEnvironment;
import graphql.parser.ParserOptions;
import graphql.validation.Validator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
                Map<String, Object> vars = args.length > 2 ? JSON.readValue(args[2], new tools.jackson.core.type.TypeReference<Map<String, Object>>() {}) : Map.of();
                var input = ExecutionInput.newExecutionInput(Files.readString(Path.of(args[1]))).variables(vars).build();
                System.out.println(JSON.writeValueAsString(graphQL.execute(input).toSpecification()));
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
        // The default parser stops at 15,000 tokens; a 5,000-field query has 15,002. Raise the cap for
        // this experiment only: the limit exists to stop parser-exhaustion attacks (Module 10).
        var options = ParserOptions.getDefaultOperationParserOptions().transform(o -> o.maxTokens(100_000));
        long t0 = System.nanoTime();
        Document doc = new Parser().parseDocument(ParserEnvironment.newParserEnvironment()
            .document(query).parserOptions(options).build());
        long t1 = System.nanoTime();
        var errors = new Validator().validateDocument(schema, doc, Locale.ENGLISH);
        long t2 = System.nanoTime();
        // Hand the already-parsed, already-validated document to execution, so only execution is timed.
        var graphQL = GraphQL.newGraphQL(schema)
            .preparsedDocumentProvider((in, parseAndValidate) -> CompletableFuture.completedFuture(new PreparsedDocumentEntry(doc)))
            .build();
        var result = graphQL.execute(ExecutionInput.newExecutionInput(query).build());
        long t3 = System.nanoTime();
        System.out.printf("parse %.3f ms%n", (t1 - t0) / 1e6);
        System.out.printf("validate %.3f ms%n", (t2 - t1) / 1e6);
        System.out.printf("execute %.3f ms%n", (t3 - t2) / 1e6);
        System.out.println("validation-errors " + errors.size());
        Map<String, Object> data = result.getData();
        System.out.println("fields " + (data == null ? 0 : data.size()));
    }
}
