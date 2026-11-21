import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
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
                .dataFetcher("members", e -> e.<Services.Club>getSource().memberIds().stream().map(clubs::user).toList())
                // TODO: when the service fails, keep the page. When it answers partially, say so.
                .dataFetcher("recentReviews", e -> reviews.recent(e.<Services.Club>getSource().id()).reviews()))
            .type("Member", t -> t.dataFetcher("isOwner", e -> {
                // TODO: a Member doesn't know its club. This works for c1 only, and costs a load per member.
                return e.<Services.User>getSource().id().equals(clubs.load("c1").ownerId());
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
