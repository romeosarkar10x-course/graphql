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
                .dataFetcher("recent", e -> Data.REVIEWS.stream()
                    .sorted(Comparator.comparing(Data.Review::createdAt).reversed()).limit(e.<Integer>getArgument("n")).toList()))
            .type("Club", t -> t
                .dataFetcher("size", e -> { var c = e.<Data.Club>getSource(); return (int) c.memberIds().stream().filter(id -> !id.equals(c.ownerId())).count(); })
                .dataFetcher("active", e -> !Data.meetupsOf(e.<Data.Club>getSource().id(), true).isEmpty())
                .dataFetcher("nextMeetup", e -> Data.meetupsOf(e.<Data.Club>getSource().id(), true).stream().findFirst().orElse(null)))
            .type("User", t -> t
                .dataFetcher("latestReview", e -> Data.reviewsBy(e.<Data.User>getSource().id()).stream()
                    .max(Comparator.comparing(Data.Review::createdAt)).orElse(null))
                .dataFetcher("since", e -> Data.reviewsBy(e.<Data.User>getSource().id()).stream()
                    .map(Data.Review::createdAt).min(Comparator.naturalOrder()).orElse(null)))
            .type("Book", t -> t
                .dataFetcher("rating", e -> {
                    String viewer = e.getGraphQlContext().get("viewer");
                    return Data.reviewsOf(e.<Data.Book>getSource().id()).stream().filter(r -> r.authorId().equals(viewer))
                        .map(r -> (double) r.rating()).findFirst().orElse(null);
                })
                .dataFetcher("popular", e -> {
                    var id = e.<Data.Book>getSource().id();
                    Double avg = Data.averageRating(id);
                    return Data.reviewsOf(id).size() >= 3 && avg != null && avg >= 4.0;
                }))
            .build();
    }

    public static void main(String[] args) throws Exception {
        var schema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(Files.readString(Path.of("relay.graphqls"))), wiring());
        var input = ExecutionInput.newExecutionInput(Files.readString(Path.of(args[0])));
        if (args.length > 1) input.graphQLContext(Map.of("viewer", args[1]));
        System.out.println(JsonMapper.builder().build().writeValueAsString(GraphQL.newGraphQL(schema).build().execute(input.build()).toSpecification()));
    }
}
