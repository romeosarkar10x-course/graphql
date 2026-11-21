import graphql.GraphQL;
import graphql.schema.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Lab 7.2 harness: GlobalId, the Node schema, and refetching through node(id:) and nodes(ids:). */
public class NodeCheck {
    static int fails = 0;
    static void check(boolean ok, String what, Object detail) {
        System.out.println((ok ? "ok   " : "fail ") + what + (ok || detail == null ? "" : "   got: " + detail));
        if (!ok) fails++;
    }
    static String b64(String s) { return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }

    @SuppressWarnings("unchecked")
    static <T> T at(Object o, Object... path) {
        for (Object p : path) {
            if (o == null) return null;
            o = p instanceof Integer i ? ((List<?>) o).get(i) : ((Map<?, ?>) o).get(p);
        }
        return (T) o;
    }

    public static void main(String[] a) throws Exception {
        // GlobalId
        for (var pair : List.of(List.of("Club", "c1"), List.of("Review", "r:15"), List.of("User", "ü-é 1"))) {
            var g = new GlobalId(pair.get(0), pair.get(1));
            check(GlobalId.decode(g.encode()).equals(Optional.of(g)), "round trip " + pair, GlobalId.decode(g.encode()));
        }
        String c1 = new GlobalId("Club", "c1").encode();
        check(!c1.contains("Club") && !c1.contains("c1") && c1.matches("[A-Za-z0-9_-]+"), "opaque, URL-safe, unpadded: " + c1, c1);
        check(c1.equals(b64("Club:c1")), "encoding is base64url of \"Type:key\" (other servers must be able to read it)", c1);
        for (String bad : List.of("", "!!!", b64("nocolon"), b64("club:c1"), b64("Club:"), b64(":c1")))
            check(GlobalId.decode(bad).isEmpty(), "decode rejects \"" + bad + "\"", GlobalId.decode(bad));

        // Schema
        GraphQL g = Nodes.graphQL();
        GraphQLSchema s = g.getGraphQLSchema();
        var node = s.getType("Node");
        check(node instanceof GraphQLInterfaceType i && i.getFieldDefinition("id") != null
            && GraphQLTypeUtil.simplePrint(i.getFieldDefinition("id").getType()).equals("ID!"), "interface Node { id: ID! }", node);
        if (node instanceof GraphQLInterfaceType i) {
            var impl = s.getImplementations(i).stream().map(GraphQLObjectType::getName).sorted().toList();
            check(impl.containsAll(List.of("Book", "Club", "Meetup", "Review", "User")), "Club, User, Book, Review and Meetup implement Node", impl);
        }
        var q = s.getQueryType();
        var nf = q.getFieldDefinition("node"); var nsf = q.getFieldDefinition("nodes");
        check(nf != null && GraphQLTypeUtil.simplePrint(nf.getType()).equals("Node") && nf.getArgument("id") != null
            && GraphQLTypeUtil.simplePrint(nf.getArgument("id").getType()).equals("ID!"), "Query.node(id: ID!): Node", nf == null ? null : GraphQLTypeUtil.simplePrint(nf.getType()));
        check(nsf != null && GraphQLTypeUtil.simplePrint(nsf.getType()).equals("[Node]!") && nsf.getArgument("ids") != null
            && GraphQLTypeUtil.simplePrint(nsf.getArgument("ids").getType()).equals("[ID!]!"), "Query.nodes(ids: [ID!]!): [Node]!", nsf == null ? null : GraphQLTypeUtil.simplePrint(nsf.getType()));

        // Ids on the wire
        Map<String, Object> r = g.execute("{ clubs { id owner { id } currentBook { id reviews { id } } meetups { id } } }").toSpecification();
        check(r.get("errors") == null, "clubs query runs", r.get("errors"));
        String clubId = at(r, "data", "clubs", 0, "id");
        check(clubId != null && GlobalId.decode(clubId).equals(Optional.of(new GlobalId("Club", "c1"))), "the first club's id decodes to Club:c1", clubId);
        Map<String, String> one = new LinkedHashMap<>();
        one.put("Club", clubId);
        one.put("User", at(r, "data", "clubs", 0, "owner", "id"));
        one.put("Book", at(r, "data", "clubs", 0, "currentBook", "id"));
        one.put("Review", at(r, "data", "clubs", 0, "currentBook", "reviews", 0, "id"));
        one.put("Meetup", at(r, "data", "clubs", 0, "meetups", 0, "id"));

        // Refetch 5 types
        for (var e : one.entrySet()) {
            var rr = g.execute(graphql.ExecutionInput.newExecutionInput("query($id: ID!) { node(id: $id) { __typename id } }")
                .variables(Map.of("id", String.valueOf(e.getValue()))).build()).toSpecification();
            check(e.getKey().equals(at(rr, "data", "node", "__typename")) && String.valueOf(e.getValue()).equals(at(rr, "data", "node", "id")),
                "node(id:) refetches a " + e.getKey() + " with the same id", rr);
        }
        var rr = g.execute(graphql.ExecutionInput.newExecutionInput("query($id: ID!) { node(id: $id) { ... on Club { name owner { name } } } }")
            .variables(Map.of("id", String.valueOf(clubId))).build()).toSpecification();
        check("The Midnight Readers".equals(at(rr, "data", "node", "name")) && "Kemi Adeyemi".equals(at(rr, "data", "node", "owner", "name")),
            "node(id:) ... on Club reads the club's fields", rr);

        // nodes(ids:): order, nulls, no type confusion
        List<String> ids = List.of(clubId, new GlobalId("Club", "zz").encode(), "garbage", String.valueOf(one.get("Book")),
            new GlobalId("Book", "c1").encode(), new GlobalId("Shelf", "1").encode());
        var nr = g.execute(graphql.ExecutionInput.newExecutionInput("query($ids: [ID!]!) { nodes(ids: $ids) { __typename } }")
            .variables(Map.of("ids", ids)).build()).toSpecification();
        List<Object> got = new ArrayList<>();
        List<?> list = at(nr, "data", "nodes");
        if (list != null) for (Object o : list) got.add(o == null ? null : ((Map<?, ?>) o).get("__typename"));
        check(Arrays.asList("Club", null, null, "Book", null, null).equals(got),
            "nodes(ids:) = [Club, null (unknown key), null (garbage), Book, null (Book:c1 is a club's key), null (unknown type)]", got);

        System.out.println(fails == 0 ? "all checks pass" : fails + " check(s) failed");
        System.exit(fails == 0 ? 0 : 1);
    }
}
