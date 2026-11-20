import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Lab 3.1: SDL file -> TypeDefinitionRegistry -> RuntimeWiring -> executable GraphQLSchema.
 *
 *   java -cp "$(lib)" ShelfieSchema.java query.graphql ['{"id":"c1"}']   run a query, print the JSON response
 *   java -cp "$(lib)" ShelfieSchema.java --audit [extra.graphqls ...]     list fields that will silently be null
 */
public class ShelfieSchema {

    /** The record behind each object type: the default fetcher reads a component with the field's name. */
    static final Map<String, Class<?>> BACKING = Map.of(
            "Club", Data.Club.class, "User", Data.User.class, "Book", Data.Book.class, "Review", Data.Review.class);

    static RuntimeWiring wiring() {
        // TODO 1: wire the rest. Fields whose name matches a record component (id, name, title, ...)
        // need nothing: graphql-java's default PropertyDataFetcher reads them. Every other field needs
        // a data fetcher here, or it is null. Read the schema and Data.java side by side.
        return RuntimeWiring.newRuntimeWiring()
                .type("Query", t -> t
                        .dataFetcher("club", env -> Data.club(env.getArgument("id")).orElse(null))
                        .dataFetcher("clubs", env -> Data.CLUBS))
                .build();
    }

    static GraphQLSchema build(List<Path> sdlFiles) throws Exception {
        TypeDefinitionRegistry registry = new TypeDefinitionRegistry();
        for (Path p : sdlFiles) registry.merge(new SchemaParser().parse(Files.readString(p)));
        return new SchemaGenerator().makeExecutableSchema(registry, wiring());
    }

    /** Fields with no registered data fetcher and no record component of the same name. */
    static List<String> audit(GraphQLSchema schema) {
        // TODO 2: for each type in BACKING, list "Type.field" for fields that have no data fetcher in
        // schema.getCodeRegistry() and no record component of that name. Sorted.
        return List.of();
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: ShelfieSchema <query.graphql> [variables-json] | --audit [extra.graphqls ...]");
            System.exit(2);
        }
        if (args[0].equals("--audit")) {
            List<Path> files = new ArrayList<>(List.of(Path.of("schema.graphqls")));
            for (int i = 1; i < args.length; i++) files.add(Path.of(args[i]));
            audit(build(files)).forEach(System.out::println);
            return;
        }
        GraphQL graphQL = GraphQL.newGraphQL(build(List.of(Path.of("schema.graphqls")))).build();
        Map<String, Object> vars = args.length > 1 ? Json.read(args[1]) : Map.of();
        var result = graphQL.execute(ExecutionInput.newExecutionInput(Files.readString(Path.of(args[0]))).variables(vars).build());
        System.out.println(Json.of(result.toSpecification()));
    }
}
