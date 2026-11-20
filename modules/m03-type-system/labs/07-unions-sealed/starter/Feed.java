import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.TypeResolver;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Lab 3.7: a GraphQL union as a Java sealed interface of records, resolved with an exhaustive switch.
 *
 *   java -cp "$(lib)" Feed.java query.graphql ['{...}']
 */
public class Feed {

    // TODO 1: make FeedItem sealed (no permits clause: the records below are in the same file).
    interface FeedItem {
        String clubId();
        String at();
    }

    record ReviewPosted(String clubId, String at, String reviewerId, String bookId, int rating) implements FeedItem {}
    record MemberJoined(String clubId, String at, String memberId) implements FeedItem {}
    record MeetupScheduled(String clubId, String at, String title, String startsAt) implements FeedItem {}
    record BookFinished(String clubId, String at, String bookId, int readers) implements FeedItem {}
    // -- new feed items go here --

    static final List<FeedItem> ITEMS = List.of(
            new MemberJoined("c1", "2026-09-01T10:00:00Z", "u10"),
            new BookFinished("c1", "2026-09-28T20:00:00Z", "b6", 6),
            new ReviewPosted("c1", "2026-10-02T21:10:00Z", "u1", "b1", 5),
            new MeetupScheduled("c1", "2026-10-20T12:00:00Z", "Left Hand of Darkness, part 2", "2026-11-27T19:00:00Z"),
            new ReviewPosted("c1", "2026-10-15T23:59:00Z", "u8", "b1", 4),
            new MemberJoined("c2", "2026-08-01T09:00:00Z", "u12"),
            new ReviewPosted("c2", "2026-08-20T20:00:00Z", "u7", "b4", 5));

    // TODO 2: a pattern-matching switch with no default, so a new record is a compile error.
    static final TypeResolver FEED_ITEM = env -> {
        Object item = env.getObject();
        String type;
        if (item instanceof ReviewPosted) type = "ReviewPosted";
        else if (item instanceof MemberJoined) type = "MemberJoined";
        else type = "MeetupScheduled";
        return env.getSchema().getObjectType(type);
    };

    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type("FeedItem", t -> t.typeResolver(FEED_ITEM))
                .type("Query", t -> t.dataFetcher("feed", env -> ITEMS.stream()
                        .filter(i -> i.clubId().equals(env.getArgument("clubId")))
                        .sorted(Comparator.comparing(FeedItem::at).reversed()).toList()))
                .type("ReviewPosted", t -> t
                        .dataFetcher("reviewer", env -> Data.user(env.<ReviewPosted>getSource().reviewerId()).orElseThrow())
                        .dataFetcher("book", env -> Data.book(env.<ReviewPosted>getSource().bookId()).orElseThrow()))
                .type("MemberJoined", t -> t
                        .dataFetcher("member", env -> Data.user(env.<MemberJoined>getSource().memberId()).orElseThrow()))
                // TODO 3: BookFinished.book
                .build();
    }

    public static void main(String[] args) throws Exception {
        var schema = new SchemaGenerator().makeExecutableSchema(
                new SchemaParser().parse(Files.readString(Path.of("schema.graphqls"))), wiring());
        Map<String, Object> vars = args.length > 1 ? Json.read(args[1]) : Map.of();
        var result = GraphQL.newGraphQL(schema).build()
                .execute(ExecutionInput.newExecutionInput(Files.readString(Path.of(args[0]))).variables(vars).build());
        System.out.println(Json.of(result.toSpecification()));
    }
}
