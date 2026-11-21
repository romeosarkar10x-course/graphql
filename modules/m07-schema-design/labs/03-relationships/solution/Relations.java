import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Lab 7.3: relationships as fields, and a members connection with edge data. The harness calls graphQL(). */
final class Relations {
    private Relations() {}

    record Edge(String cursor, Data.User node, String role, String joinedAt) {}
    record PageInfo(boolean hasNextPage, String endCursor) {}
    record Connection(List<Edge> edges, PageInfo pageInfo, int totalCount) {}

    static String cursor(int index) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(("member:" + index).getBytes(StandardCharsets.UTF_8));
    }

    /** The index after `after`, or 0. A cursor we did not make is a client error. */
    static int start(String after) {
        if (after == null) return 0;
        String raw = new String(Base64.getUrlDecoder().decode(after), StandardCharsets.UTF_8);
        if (!raw.startsWith("member:")) throw new IllegalArgumentException("Not a member cursor: " + after);
        return Integer.parseInt(raw.substring("member:".length())) + 1;
    }

    static Connection members(DataFetchingEnvironment env) {
        var all = Memberships.of(env.<Data.Club>getSource().id());
        int from = Math.min(start(env.getArgument("after")), all.size());
        int first = Math.max(0, env.<Integer>getArgument("first"));
        int to = Math.min(all.size(), from + first);
        List<Edge> edges = new java.util.ArrayList<>();
        for (int i = from; i < to; i++) {
            var m = all.get(i);
            edges.add(new Edge(cursor(i), Data.user(m.userId()).orElseThrow(), m.role().name(), m.joinedAt()));
        }
        return new Connection(edges, new PageInfo(to < all.size(), edges.isEmpty() ? null : edges.getLast().cursor()), all.size());
    }

    static GraphQL graphQL() throws Exception {
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("club", env -> Data.club(env.getArgument("id")).orElse(null))
                .dataFetcher("user", env -> Data.user(env.getArgument("id")).orElse(null))
                .dataFetcher("book", env -> Data.book(env.getArgument("id")).orElse(null))
                .dataFetcher("review", env -> Data.review(env.getArgument("id")).orElse(null)))
            .type("Club", t -> t
                .dataFetcher("owner", env -> Data.user(env.<Data.Club>getSource().ownerId()).orElseThrow())
                .dataFetcher("currentBook", env -> Data.book(env.<Data.Club>getSource().currentBookId()).orElse(null))
                .dataFetcher("members", Relations::members))
            .type("User", t -> t.dataFetcher("clubs", env -> Data.clubsOf(env.<Data.User>getSource().id())))
            .type("Review", t -> t
                .dataFetcher("author", env -> Data.user(env.<Data.Review>getSource().authorId()).orElseThrow())
                .dataFetcher("book", env -> Data.book(env.<Data.Review>getSource().bookId()).orElseThrow()))
            .build();
        var sdl = Files.readString(Path.of("relations.graphqls"));
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), wiring)).build();
    }
}
