import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;

/** Lab 7.3: relationships as fields, and a members connection with edge data. The harness calls graphQL(). */
final class Relations {
    private Relations() {}

    static GraphQL graphQL() throws Exception {
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t
                .dataFetcher("club", env -> Data.club(env.getArgument("id")).orElse(null))
                .dataFetcher("user", env -> Data.user(env.getArgument("id")).orElse(null))
                .dataFetcher("book", env -> Data.book(env.getArgument("id")).orElse(null))
                .dataFetcher("review", env -> Data.review(env.getArgument("id")).orElse(null)))
            // The key-shaped schema needs these; the redesign replaces them with fields that return objects.
            .type("Club", t -> t
                .dataFetcher("memberIds", env -> env.<Data.Club>getSource().memberIds())
                .dataFetcher("memberRoles", env -> Memberships.of(env.<Data.Club>getSource().id()).stream().map(m -> m.role().name()).toList()))
            .type("User", t -> t.dataFetcher("clubIds", env -> Data.clubsOf(env.<Data.User>getSource().id()).stream().map(Data.Club::id).toList()))
            .build();
        var sdl = Files.readString(Path.of("relations.graphqls"));
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), wiring)).build();
    }
}
