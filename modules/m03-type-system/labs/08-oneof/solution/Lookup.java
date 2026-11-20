import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Lab 3.8: a @oneOf input. graphql-java enforces "exactly one non-null key" before the fetcher runs,
 * so the fetcher can switch on the one key it gets.
 *
 *   java -cp "$(lib)" Lookup.java query.graphql ['{...}']
 *   java -cp "$(lib)" Lookup.java --schema other.graphqls      only build the schema; print BUILT or the error
 */
public class Lookup {

    static Optional<Data.Book> find(Map<String, Object> by) {
        var entry = by.entrySet().iterator().next();   // @oneOf: exactly one, and it is non-null
        String v = (String) entry.getValue();
        return switch (entry.getKey()) {
            case "id" -> Data.book(v);
            case "isbn" -> Data.BOOKS.stream().filter(b -> b.isbn().equals(v)).findFirst();
            case "title" -> Data.BOOKS.stream().filter(b -> b.title().toLowerCase(Locale.ROOT).equals(v.toLowerCase(Locale.ROOT))).findFirst();
            default -> throw new IllegalStateException("BookLookup has no key " + entry.getKey());
        };
    }

    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type("Query", t -> t.dataFetcher("book", env -> find(env.getArgument("by")).orElse(null)))
                .build();
    }

    public static void main(String[] args) throws Exception {
        if (args[0].equals("--schema")) {
            try {
                new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(Files.readString(Path.of(args[1]))), wiring());
                System.out.println("BUILT");
            } catch (Exception e) {
                System.out.println("REJECTED " + e.getMessage().replaceAll("\\s+", " ").trim());
            }
            return;
        }
        var schema = new SchemaGenerator().makeExecutableSchema(
                new SchemaParser().parse(Files.readString(Path.of("schema.graphqls"))), wiring());
        Map<String, Object> vars = args.length > 1 ? Json.read(args[1]) : Map.of();
        var result = GraphQL.newGraphQL(schema).build()
                .execute(ExecutionInput.newExecutionInput(Files.readString(Path.of(args[0]))).variables(vars).build());
        System.out.println(Json.of(result.toSpecification()));
    }
}
