import graphql.schema.*;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Lab 7.4 harness: the audit's decisions, then four outages. Compiled together with tools/design/Design.java. */
public class AuditCheck {
    static int fails = 0;
    static void check(boolean ok, String what) { System.out.println((ok ? "ok   " : "fail ") + what); if (!ok) fails++; }

    public static void main(String[] a) throws Exception {
        String sdl = Files.readString(Path.of("club.graphqls")), query = Files.readString(Path.of("club-page.graphql"));
        GraphQLSchema s = UnExecutableSchemaGenerator.makeUnExecutableSchema(Design.registry(sdl));

        // Promises you can keep: the value comes with the parent's row and has no service of its own.
        for (String c : List.of("Club.id", "Club.name", "Club.owner", "User.id", "Book.id", "Book.title", "Book.authors",
                                "Author.id", "Author.name", "Meetup.id", "Meetup.startsAt", "Meetup.venue"))
            check(nonNull(s, c), c + " stays non-null (it comes with its parent; making it nullable only adds client null checks)");
        check(!nonNull(s, "Query.club"), "Query.club is nullable (an unknown id is not an error)");
        // Absent in real data: nullable, and not semantically non-null.
        for (String c : List.of("Club.description", "Club.viewerIsMember", "Club.currentBook", "Club.nextMeetup",
                                "User.avatarUrl", "Book.coverUrl", "Book.averageRating"))
            check(!nonNull(s, c) && !semantic(s, c), c + " is nullable, without @semanticNonNull (null is a real answer)");
        // Never absent, but served by a dependency that can fail: nullable for the error's sake, and marked.
        for (String c : List.of("User.name", "Club.memberCount", "Club.members"))
            check(!nonNull(s, c) && semantic(s, c), c + " is nullable with @semanticNonNull (null only on error)");

        // The outages.
        record Outage(String name, Set<String> down, int atLeast) {}
        for (var o : List.of(new Outage("ratings service", Set.of("Book.averageRating"), 24),
                             new Outage("profile service", Set.of("User.name", "User.avatarUrl"), 19),
                             new Outage("meetup service", Set.of("Club.nextMeetup"), 22),
                             new Outage("membership service", Set.of("Club.viewerIsMember", "Club.memberCount", "Club.members"), 17))) {
            var r = Design.run(Design.mock(sdl, o.down()), query);
            int left = Design.leaves(Design.JSON.valueToTree(r.get("data")));
            check(left >= o.atLeast(), o.name() + " down: " + left + " of 25 values survive (only the failed fields may go: " + o.atLeast() + ")");
        }
        System.out.println(fails == 0 ? "all checks pass" : fails + " check(s) failed");
        System.exit(fails == 0 ? 0 : 1);
    }

    static GraphQLFieldDefinition field(GraphQLSchema s, String coord) {
        String[] p = coord.split("\\.");
        return s.getType(p[0]) instanceof GraphQLFieldsContainer c ? c.getFieldDefinition(p[1]) : null;
    }
    static boolean nonNull(GraphQLSchema s, String coord) { var f = field(s, coord); return f != null && GraphQLTypeUtil.isNonNull(f.getType()); }
    static boolean semantic(GraphQLSchema s, String coord) { var f = field(s, coord); return f != null && f.hasAppliedDirective("semanticNonNull"); }
}
