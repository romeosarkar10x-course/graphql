import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.EnumValuesProvider;
import graphql.schema.idl.NaturalEnumValuesProvider;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Lab 3.5: one enum backed by legacy database codes, one by a Java enum.
 *
 *   java -cp "$(lib)" Enums.java [--v2] query.graphql ['{...}']   --v2 serves schema-v2.graphqls
 */
public class Enums {

    /** Rows as the legacy clubs table stores them: visibility is a one-letter code. */
    record ClubRow(String id, String name, String visibility) {}

    /** The Java side has the full v2 enum: the database may already contain ABANDONED. */
    enum ReadingStatus { WANT_TO_READ, READING, FINISHED, ABANDONED }

    record ShelfEntry(String userId, String bookId, ReadingStatus status) {}

    static final List<ClubRow> CLUBS = List.of(
            new ClubRow("c1", "The Midnight Readers", "P"),
            new ClubRow("c2", "Slow Burn Mysteries", "P"),
            new ClubRow("c3", "Booker Shortlist Survivors", "X"));

    static final List<ShelfEntry> SHELF = List.of(
            new ShelfEntry("u1", "b1", ReadingStatus.FINISHED),
            new ShelfEntry("u1", "b6", ReadingStatus.READING),
            new ShelfEntry("u1", "b10", ReadingStatus.WANT_TO_READ),
            new ShelfEntry("u3", "b9", ReadingStatus.FINISHED),
            new ShelfEntry("u3", "b4", ReadingStatus.ABANDONED));

    /** GraphQL name <-> legacy code, both directions: arguments arrive as codes, codes serialise as names. */
    static final EnumValuesProvider VISIBILITY_CODES = name -> switch (name) {
        case "PUBLIC" -> "P";
        case "PRIVATE" -> "X";
        default -> null;
    };

    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type("Visibility", t -> t.enumValues(VISIBILITY_CODES))
                .type("ReadingStatus", t -> t.enumValues(new NaturalEnumValuesProvider<>(ReadingStatus.class)))
                .type("Query", t -> t
                        .dataFetcher("clubs", env -> {
                            String code = env.getArgument("visibility");   // "P", "X" or null
                            return CLUBS.stream().filter(c -> code == null || c.visibility().equals(code)).toList();
                        })
                        .dataFetcher("shelf", env -> SHELF.stream().filter(s -> s.userId().equals(env.getArgument("userId"))).toList()))
                .build();
    }

    public static void main(String[] args) throws Exception {
        boolean v2 = args[0].equals("--v2");
        int i = v2 ? 1 : 0;
        var schema = new SchemaGenerator().makeExecutableSchema(
                new SchemaParser().parse(Files.readString(Path.of(v2 ? "schema-v2.graphqls" : "schema.graphqls"))), wiring());
        Map<String, Object> vars = args.length > i + 1 ? Json.read(args[i + 1]) : Map.of();
        var result = GraphQL.newGraphQL(schema).build()
                .execute(ExecutionInput.newExecutionInput(Files.readString(Path.of(args[i]))).variables(vars).build());
        System.out.println(Json.of(result.toSpecification()));
    }
}
