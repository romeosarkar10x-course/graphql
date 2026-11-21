import graphql.ErrorClassification;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import tools.jackson.databind.json.JsonMapper;

/** Lab 5.6: every exception becomes a deliberate GraphQL error. java Errors.java run <query.graphql> */
public class Errors {

    /** Our own classifications; graphql-java prints the enum name as extensions.classification. */
    enum ShelfieError implements ErrorClassification { NOT_FOUND, FORBIDDEN, BAD_USER_INPUT, UNAVAILABLE, INTERNAL }

    static final class Handler implements DataFetcherExceptionHandler {
        @Override
        public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(DataFetcherExceptionHandlerParameters p) {
            Throwable e = p.getException();
            // GraphqlErrorBuilder.newError(env) fills in path and locations from the field that failed.
            var b = GraphqlErrorBuilder.newError(p.getDataFetchingEnvironment());
            var ext = new LinkedHashMap<String, Object>();
            switch (e) {
                case Problems.NotFoundException nf -> b.message(nf.getMessage()).errorType(ShelfieError.NOT_FOUND);
                case Problems.ForbiddenException f -> b.message(f.getMessage()).errorType(ShelfieError.FORBIDDEN);
                case Problems.InvalidInputException in -> {
                    b.message(in.getMessage()).errorType(ShelfieError.BAD_USER_INPUT);
                    ext.put("field", in.field);
                }
                case TimeoutException t -> {
                    // Say what is unavailable and that a retry may work; do not echo the internal service name.
                    b.message("A service Shelfie depends on is not answering").errorType(ShelfieError.UNAVAILABLE);
                    ext.put("retryable", true);
                }
                default -> {
                    // A bug. The client gets an id to quote; the log gets the details. Never the reverse.
                    String id = UUID.randomUUID().toString().substring(0, 8);
                    System.err.println("INTERNAL " + id + " at " + p.getPath() + ": " + e);
                    b.message("Internal error (ref " + id + ")").errorType(ShelfieError.INTERNAL);
                    ext.put("ref", id);
                }
            }
            GraphQLError error = b.extensions(ext).build();
            return CompletableFuture.completedFuture(DataFetcherExceptionHandlerResult.newResult(error).build());
        }
    }

    public static void main(String[] args) throws Exception {
        var types = new TypeDefinitionRegistry();
        types.merge(new SchemaParser().parse(Files.readString(Path.of("shelfie.graphqls"))));
        types.merge(new SchemaParser().parse(Files.readString(Path.of("lab.graphqls"))));
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("hello", env -> "Hello from Shelfie")
                .dataFetcher("club", env -> Data.club(env.getArgument("id")).orElse(null))
                .dataFetcher("trouble", env -> Problems.trouble(env.getArgument("kind"))))
            .type("Club", t -> t
                .dataFetcher("trouble", env -> Problems.trouble(env.getArgument("kind")))
                .dataFetcher("mustWork", env -> Problems.trouble(env.getArgument("kind"))))
            .build();
        var graphQL = GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(types, wiring))
            .defaultDataFetcherExceptionHandler(new Handler())
            .build();
        var result = graphQL.execute(ExecutionInput.newExecutionInput(Files.readString(Path.of(args[1]))).build());
        System.out.println(JsonMapper.builder().build().writeValueAsString(result.toSpecification()));
    }
}
