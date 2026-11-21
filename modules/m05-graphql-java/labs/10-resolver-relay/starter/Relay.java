import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** Game 5a, resolver relay: fetchers for relay.graphqls. java Relay.java <query.graphql> [viewerId] */
public class Relay {
    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("club", e -> Data.club(e.getArgument("id")).orElse(null))
                .dataFetcher("user", e -> Data.user(e.getArgument("id")).orElse(null))
                .dataFetcher("book", e -> Data.book(e.getArgument("id")).orElse(null))
                .dataFetcher("recent", e -> java.util.List.of()))   // TODO
            // TODO: implement every field below from its name and type alone, writing your guess in guesses.md
            // first. Only then open ../INTENT.md (the lab's, not your copy) and reconcile.
            .type("Club", t -> t
                .dataFetcher("size", e -> 0)
                .dataFetcher("active", e -> false)
                .dataFetcher("nextMeetup", e -> null))
            .type("User", t -> t
                .dataFetcher("latestReview", e -> null)
                .dataFetcher("since", e -> null))
            .type("Book", t -> t
                .dataFetcher("rating", e -> null)
                .dataFetcher("popular", e -> false))
            .build();
    }

    public static void main(String[] args) throws Exception {
        var schema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(Files.readString(Path.of("relay.graphqls"))), wiring());
        var input = ExecutionInput.newExecutionInput(Files.readString(Path.of(args[0])));
        if (args.length > 1) input.graphQLContext(Map.of("viewer", args[1]));
        System.out.println(JsonMapper.builder().build().writeValueAsString(GraphQL.newGraphQL(schema).build().execute(input.build()).toSpecification()));
    }
}
