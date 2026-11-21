import graphql.GraphQL;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 8.7: the club page renders even when the reviews service doesn't.
 *   bin/course java ClubPage.java [UP|DOWN|PARTIAL]
 */
public class ClubPage {
    public static final String SDL = """
        type Query { club(id: ID!): Club }
        type Club {
          id: ID!
          name: String!
          members: [Member!]!
          "Newest first. When the reviews service fails this is empty or short, and errors say why."
          recentReviews: [Review!]!
        }
        type Member { id: ID! name: String! isOwner: Boolean! }
        type Review { id: ID! rating: Int! body: String! author: Member! }
        """;

    public static final String PAGE = "{ club(id: \"c1\") { name members { name isOwner } recentReviews { rating body author { name isOwner } } } }";

    public static GraphQL graphQL(Services.ClubService clubs, Services.ReviewsService reviews) {
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t.dataFetcher("club", e -> clubs.load(e.getArgument("id"))))
            .type("Club", t -> t
                // The club is the local context of everything below it: Member.isOwner needs the owner id,
                // and reading it from here costs nothing, where clubs.load(...) again would cost a call.
                .dataFetcher("members", e -> {
                    Services.Club club = e.getSource();
                    return DataFetcherResult.newResult().data(club.memberIds().stream().map(clubs::user).toList()).localContext(club).build();
                })
                .dataFetcher("recentReviews", e -> {
                    Services.Club club = e.getSource();
                    Services.Batch batch;
                    try {
                        batch = reviews.recent(club.id());
                    } catch (Services.ReviewsUnavailableException ex) {
                        // Data AND an error: an empty list keeps the non-null field, so the club survives.
                        return DataFetcherResult.newResult().data(List.of()).localContext(club)
                            .error(GraphqlErrorBuilder.newError(e).message("Reviews are unavailable right now")
                                .extensions(Map.of("code", "REVIEWS_UNAVAILABLE")).build())
                            .build();
                    }
                    var result = DataFetcherResult.newResult().data(batch.reviews()).localContext(club);
                    // One error for the position, listing the missing ids: since RFC #1183 (accepted
                    // 2026-09-17) an error's path should be unique in the errors list.
                    if (!batch.failedIds().isEmpty()) {
                        result.error(GraphqlErrorBuilder.newError(e).message(batch.failedIds().size() + " reviews could not be loaded")
                            .extensions(Map.of("code", "REVIEWS_INCOMPLETE", "missing", batch.failedIds())).build());
                    }
                    return result.build();
                }))
            .type("Member", t -> t.dataFetcher("isOwner", e -> {
                Services.Club club = e.getLocalContext();          // set by members or recentReviews above
                return e.<Services.User>getSource().id().equals(club.ownerId());
            }))
            .type("Review", t -> t.dataFetcher("author", e -> clubs.user(e.<Services.Review>getSource().authorId())))
            .build();
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(SDL), wiring)).build();
    }

    public static void main(String[] args) {
        var clubs = new Services.ClubService();
        var reviews = new Services.ReviewsService();
        if (args.length > 0) reviews.mode = Services.Mode.valueOf(args[0]);
        var result = graphQL(clubs, reviews).execute(PAGE).toSpecification();
        System.out.println(JsonMapper.builder().build().writeValueAsString(result));
        System.out.println("club loads: " + clubs.loads.get() + ", reviews calls: " + reviews.calls.get());
    }
}
