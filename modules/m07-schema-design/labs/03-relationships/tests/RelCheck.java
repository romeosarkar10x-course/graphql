import graphql.ExecutionInput;
import graphql.GraphQL;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Lab 7.3 harness: prints the data of fixed queries, one JSON line each, for a diff against expected/. */
public class RelCheck {
    static final JsonMapper JSON = JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
    static GraphQL g;

    static Map<String, Object> run(String label, String query, Map<String, Object> vars) {
        var r = g.execute(ExecutionInput.newExecutionInput(query).variables(vars).build()).toSpecification();
        String line = JSON.writeValueAsString(r.containsKey("errors") ? Map.of("errors", "yes", "data", String.valueOf(r.get("data"))) : r.get("data"));
        System.out.println(label + " " + line.replaceAll("\"endCursor\":(null|\"[^\"]*\"),?", ""));   // cursors are yours to design
        return r;
    }

    @SuppressWarnings("unchecked")
    static String endCursor(Map<String, Object> r) {
        try {
            var club = (Map<String, Object>) ((Map<String, Object>) r.get("data")).get("club");
            return (String) ((Map<String, Object>) ((Map<String, Object>) club.get("members")).get("pageInfo")).get("endCursor");
        } catch (RuntimeException e) { return null; }
    }

    public static void main(String[] a) throws Exception {
        g = Relations.graphQL();
        String page = "query($after: String, $first: Int) { club(id: \"c1\") { members(first: $first, after: $after) "
            + "{ totalCount edges { role joinedAt node { handle } } pageInfo { hasNextPage endCursor } } } }";
        run("club", "{ club(id: \"c1\") { name owner { handle } currentBook { title } } }", Map.of());
        var p1 = run("page1", page, Map.of("first", 3));
        String c1 = endCursor(p1);
        var p2 = run("page2", page, c1 == null ? Map.of("first", 3) : Map.of("first", 3, "after", c1));
        String c2 = endCursor(p2);
        run("page3", page, c2 == null ? Map.of("first", 10) : Map.of("first", 10, "after", c2));
        System.out.println("cursors-differ " + (c1 != null && c2 != null && !c1.equals(c2)));
        run("c2-default-page", "{ club(id: \"c2\") { members { totalCount edges { role node { name } } pageInfo { hasNextPage } } } }", Map.of());
        run("review", "{ review(id: \"r1\") { rating author { handle } book { title } } }", Map.of());
        run("user-clubs", "{ user(id: \"u1\") { clubs { name } } }", Map.of());
    }
}
