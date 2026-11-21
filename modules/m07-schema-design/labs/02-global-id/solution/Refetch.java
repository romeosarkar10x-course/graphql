import java.util.List;
import java.util.Map;

/** Lab 7.2 experiment: refetch five types through node(id:). */
class Refetch {
    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        var g = Nodes.graphQL();
        var r = g.execute("{ clubs { id owner { id } currentBook { id reviews { id } } meetups { id } } }").toSpecification();
        var club = ((List<Map<String, Object>>) ((Map<String, Object>) r.get("data")).get("clubs")).getFirst();
        var book = (Map<String, Object>) club.get("currentBook");
        var ids = List.of(club.get("id"), ((Map<?, ?>) club.get("owner")).get("id"), book.get("id"),
            ((List<Map<?, ?>>) book.get("reviews")).getFirst().get("id"), ((List<Map<?, ?>>) club.get("meetups")).getFirst().get("id"));
        for (Object id : ids) {
            var n = g.execute(graphql.ExecutionInput.newExecutionInput("query($id: ID!) { node(id: $id) { __typename id } }")
                .variables(Map.of("id", id)).build()).toSpecification();
            System.out.println(id + " -> " + ((Map<?, ?>) n.get("data")).get("node"));
        }
    }
}
