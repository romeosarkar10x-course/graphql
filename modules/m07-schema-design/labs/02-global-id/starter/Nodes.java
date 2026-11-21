import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Lab 7.2: node(id:) and nodes(ids:) over a registry of types. The harness calls graphQL(). */
final class Nodes {
    private Nodes() {}

    // TODO a registry: for each Node type, its Java class (Data.Club, ...), how to load it by key
    // (Data::club, ...) and how to read its key (Data.Club::id, ...). One map, five entries.

    static GraphQL graphQL() throws Exception {
        var wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t.dataFetcher("clubs", env -> Data.CLUBS))
            .type("Club", t -> t
                .dataFetcher("owner", env -> Data.user(env.<Data.Club>getSource().ownerId()).orElseThrow())
                .dataFetcher("currentBook", env -> Data.book(env.<Data.Club>getSource().currentBookId()).orElse(null))
                .dataFetcher("meetups", env -> Data.meetupsOf(env.<Data.Club>getSource().id(), false)))
            .type("Book", t -> t.dataFetcher("reviews", env -> Data.reviewsOf(env.<Data.Book>getSource().id())))
            .type("Review", t -> t.dataFetcher("author", env -> Data.user(env.<Data.Review>getSource().authorId()).orElseThrow()))
            // TODO every Node type's id fetcher returns a global id; node, nodes and Node's type resolver
            .build();
        var sdl = Files.readString(Path.of("nodes.graphqls"));
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), wiring)).build();
    }
}
