import graphql.ExecutionInput;
import graphql.ParseAndValidate;
import graphql.schema.*;
import graphql.schema.diff.SchemaDiff;
import graphql.schema.diff.SchemaDiffSet;
import graphql.schema.diff.reporting.CapturingReporter;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Lab 7.5 harness: three feed designs, v1 and v2. Compiled together with tools/design/Design.java. */
public class FeedCheck {
    static int fails = 0;
    static void check(boolean ok, String what) { System.out.println((ok ? "ok   " : "fail ") + what); if (!ok) fails++; }
    static GraphQLSchema schema(String sdl) { return UnExecutableSchemaGenerator.makeUnExecutableSchema(Design.registry(sdl)); }
    static String read(String f) throws Exception { return Files.exists(Path.of(f)) ? Files.readString(Path.of(f)) : ""; }

    static boolean valid(GraphQLSchema s, String doc) {
        return !doc.isBlank() && !ParseAndValidate.parseAndValidate(s, ExecutionInput.newExecutionInput(doc).build()).isFailure();
    }

    static List<String> members(GraphQLSchema s, String name) {
        return switch (s.getType(name)) {
            case GraphQLInterfaceType i -> s.getImplementations(i).stream().map(GraphQLObjectType::getName).sorted().toList();
            case GraphQLUnionType u -> u.getTypes().stream().map(GraphQLNamedType::getName).sorted().toList();
            case null, default -> List.of();
        };
    }

    public static void main(String[] a) throws Exception {
        List<String> v1Kinds = List.of("MeetupScheduled", "MemberJoined", "ReviewPosted");
        List<String> v2Kinds = List.of("BookFinished", "MeetupScheduled", "MemberJoined", "ReviewPosted");
        List<String> report = new ArrayList<>();
        for (String k : List.of("interface", "union", "enum")) {
            String s1 = read("feed-" + k + ".graphqls"), s2 = read("v2/feed-" + k + ".graphqls");
            String c1 = read("feed-" + k + ".graphql"), c2 = read("v2/feed-" + k + ".graphql");
            if (s1.isBlank() || s2.isBlank()) { check(false, k + ": feed-" + k + ".graphqls and v2/feed-" + k + ".graphqls both exist"); continue; }
            GraphQLSchema v1, v2;
            try { v1 = schema(s1); v2 = schema(s2); } catch (Exception e) { String m = String.valueOf(e.getMessage()).replaceAll("\\s+", " ");
                check(false, k + ": both schemas build (" + m.substring(0, Math.min(110, m.length())) + "...)"); continue; }
            var feed = v1.getQueryType().getFieldDefinition("feed");
            check(feed != null && GraphQLTypeUtil.simplePrint(feed.getType()).equals("[FeedItem!]!"), k + ": Query.feed returns [FeedItem!]!");
            switch (k) {
                case "interface" -> {
                    check(v1.getType("FeedItem") instanceof GraphQLInterfaceType i && i.getFieldDefinition("happenedAt") != null && i.getFieldDefinition("actor") != null,
                        "interface: FeedItem is an interface with the shared fields (happenedAt, actor)");
                    check(members(v1, "FeedItem").equals(v1Kinds) && members(v2, "FeedItem").equals(v2Kinds), "interface: v1 has 3 implementations, v2 adds BookFinished");
                }
                case "union" -> check(v1.getType("FeedItem") instanceof GraphQLUnionType && members(v1, "FeedItem").equals(v1Kinds)
                    && members(v2, "FeedItem").equals(v2Kinds), "union: FeedItem = the 3 kinds in v1, plus BookFinished in v2");
                default -> {
                    boolean ok = v1.getType("FeedItem") instanceof GraphQLObjectType o && o.getFieldDefinition("kind") != null
                        && GraphQLTypeUtil.unwrapAll(o.getFieldDefinition("kind").getType()) instanceof GraphQLEnumType e1 && e1.getValues().size() == 3
                        && v2.getType(e1.getName()) instanceof GraphQLEnumType e2 && e2.getValues().size() == 4;
                    check(ok, "enum: FeedItem is one object type with kind: an enum of 3 values in v1, 4 in v2");
                }
            }
            check(valid(v1, c1), k + ": feed-" + k + ".graphql is valid against v1");
            check(valid(v2, c1), k + ": the v1 client is still valid against v2 (no deployed client breaks)");
            check(valid(v2, c2) && (c2.contains("BookFinished") || c2.contains("book")), k + ": v2/feed-" + k + ".graphql is valid and shows the new kind");
            var r = new CapturingReporter();
            new SchemaDiff().diffSchema(SchemaDiffSet.diffSetFromSdl(s1, s2), r);
            check(r.getBreakageCount() == 0, k + ": SchemaDiff v1 -> v2 finds nothing BREAKING");
            // What an old client shows for the new kind, on mock data.
            var res = Design.run(Design.mock(s2, Set.of()), c1);
            List<?> items = res.get("data") instanceof Map<?, ?> d && d.get("feed") instanceof List<?> l ? l : List.of();
            long blank = items.stream().filter(i -> i instanceof Map<?, ?> m && m.keySet().stream().allMatch("__typename"::equals)).count();
            int changed = (int) c2.lines().filter(line -> !c1.lines().toList().contains(line)).count();
            report.add(String.format("%-9s  diff: %-9s  client lines added for v2: %d  old client on v2: %d of %d mock items blank",
                k, r.getBreakageCount() > 0 ? "BREAKING" : r.getDangerCount() > 0 ? "DANGEROUS" : "SAFE", changed, blank, items.size()));
        }
        report.forEach(l -> System.out.println("info " + l));
        System.out.println(fails == 0 ? "all checks pass" : fails + " check(s) failed");
        System.exit(fails == 0 ? 0 : 1);
    }
}
