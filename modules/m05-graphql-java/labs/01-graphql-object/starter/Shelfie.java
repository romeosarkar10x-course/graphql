import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * A complete, plain wiring of shelfie.graphqls to Data.java. Provided in labs whose topic is not the
 * wiring itself. `decorate` wraps every fetcher (instrumentation and error labs use it); pass
 * UnaryOperator.identity() for none. The viewer comes from GraphQLContext key "viewer".
 */
final class Shelfie {
    private Shelfie() {}

    static GraphQLSchema schema(Path sdl) throws Exception { return schema(sdl, f -> f); }

    static GraphQLSchema schema(Path sdl, UnaryOperator<DataFetcher<?>> decorate) throws Exception {
        var types = new SchemaParser().parse(Files.readString(sdl));
        return new SchemaGenerator().makeExecutableSchema(types, wiring(decorate));
    }

    static RuntimeWiring wiring(UnaryOperator<DataFetcher<?>> d) {
        return RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("hello", d.apply(env -> "Hello from Shelfie"))
                .dataFetcher("club", d.apply(env -> Data.club(env.getArgument("id")).orElse(null)))
                .dataFetcher("clubs", d.apply(env -> Data.CLUBS))
                .dataFetcher("user", d.apply(env -> Data.user(env.getArgument("id")).orElse(null)))
                .dataFetcher("book", d.apply(env -> Data.book(env.getArgument("id")).orElse(null)))
                .dataFetcher("books", d.apply(Shelfie::books))
                .dataFetcher("viewer", d.apply(env -> {
                    String id = env.getGraphQlContext().get("viewer");
                    return id == null ? null : Data.user(id).orElse(null);
                }))
                .dataFetcher("boom", d.apply(env -> { throw new IllegalStateException("boom"); })))
            .type("Mutation", t -> t.dataFetcher("createReview", d.apply(Shelfie::createReview)))
            .type("Club", t -> t
                .dataFetcher("owner", d.apply(env -> Data.user(env.<Data.Club>getSource().ownerId()).orElseThrow()))
                .dataFetcher("members", d.apply(env -> {
                    Data.Club c = env.getSource();
                    int first = env.getArgumentOrDefault("first", 10);
                    return c.memberIds().stream().limit(Math.max(0, first)).map(id -> Data.user(id).orElseThrow()).toList();
                }))
                .dataFetcher("memberCount", d.apply(env -> env.<Data.Club>getSource().memberIds().size()))
                .dataFetcher("currentBook", d.apply(env -> Data.book(env.<Data.Club>getSource().currentBookId()).orElse(null)))
                .dataFetcher("meetups", d.apply(env -> Data.meetupsOf(env.<Data.Club>getSource().id(),
                    env.getArgumentOrDefault("upcomingOnly", true)))))
            .type("User", t -> t
                .dataFetcher("clubs", d.apply(env -> Data.clubsOf(env.<Data.User>getSource().id())))
                .dataFetcher("reviews", d.apply(env -> Data.reviewsBy(env.<Data.User>getSource().id()))))
            .type("Book", t -> t
                .dataFetcher("averageRating", d.apply(env -> Data.averageRating(env.<Data.Book>getSource().id())))
                .dataFetcher("reviews", d.apply(env -> {
                    int last = env.getArgumentOrDefault("last", 5);
                    return Data.reviewsOf(env.<Data.Book>getSource().id()).stream().limit(Math.max(0, last)).toList();
                })))
            .type("Review", t -> t
                .dataFetcher("author", d.apply(env -> Data.user(env.<Data.Review>getSource().authorId()).orElseThrow()))
                .dataFetcher("book", d.apply(env -> Data.book(env.<Data.Review>getSource().bookId()).orElseThrow())))
            .type("Meetup", t -> t
                .dataFetcher("club", d.apply(env -> Data.club(env.<Data.Meetup>getSource().clubId()).orElseThrow())))
            .build();
    }

    static List<Data.Book> books(DataFetchingEnvironment env) {
        String text = env.getArgument("titleContains");
        int first = env.getArgumentOrDefault("first", 20);
        return Data.BOOKS.stream()
            .filter(b -> text == null || b.title().toLowerCase().contains(text.toLowerCase()))
            .limit(Math.max(0, first)).toList();
    }

    static Map<String, Object> createReview(DataFetchingEnvironment env) {
        Map<String, Object> in = env.getArgument("input");
        String viewer = env.getGraphQlContext().get("viewer");
        String bookId = (String) in.get("bookId");
        int rating = (Integer) in.get("rating");
        String body = (String) in.get("body");
        if (viewer == null) return userError(null, "Sign in to review (X-Shelfie-User header)");
        if (Data.book(bookId).isEmpty()) return userError(List.of("input", "bookId"), "No book " + bookId);
        if (rating < 1 || rating > 5) return userError(List.of("input", "rating"), "Rating must be 1 to 5");
        return Map.of("review", Data.addReview(bookId, viewer, rating, body), "userErrors", List.of());
    }

    private static Map<String, Object> userError(List<String> field, String message) {
        var e = new java.util.HashMap<String, Object>();
        e.put("field", field);
        e.put("message", message);
        var p = new java.util.HashMap<String, Object>();
        p.put("review", null);
        p.put("userErrors", List.of(e));
        return p;
    }
}
