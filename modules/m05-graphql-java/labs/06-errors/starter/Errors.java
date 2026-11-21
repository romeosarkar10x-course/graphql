import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import tools.jackson.databind.json.JsonMapper;

/** Lab 5.6: every exception becomes a deliberate GraphQL error. java Errors.java run <query.graphql> */
public class Errors {

    /**
     * TODO: graphql-java's default handler turns every exception into
     * "Exception while fetching data (/path) : <exception message>", classification DataFetchingException.
     * Write a DataFetcherExceptionHandler that gives each exception in Problems.java the error the README
     * table asks for, and register it below.
     */

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
            .build();
        var result = graphQL.execute(ExecutionInput.newExecutionInput(Files.readString(Path.of(args[1]))).build());
        System.out.println(JsonMapper.builder().build().writeValueAsString(result.toSpecification()));
    }
}
