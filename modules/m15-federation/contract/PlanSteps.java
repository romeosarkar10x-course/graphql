// Reduce a router's query plan to the order in which subgraphs are called: a list of steps, each the set of
// subgraphs called at the same time. Sequence nodes add steps; Parallel nodes merge their branches step by step;
// Flatten (an entity fetch at a path) counts as its Fetch.
//
//   java PlanSteps.java response.json          a GraphQL response with the plan in its extensions
//                                               (Apollo Router: Apollo-Expose-Query-Plan: true;
//                                                Hive Router: hive-expose-query-plan: true)
// prints {"steps": [["reviews"], ["accounts", "catalog"]], "fetches": 3}
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class PlanSteps {
  static int fetches;

  public static void main(String[] args) throws Exception {
    JsonMapper json = JsonMapper.builder().build();
    JsonNode response = json.readTree(Files.readString(Path.of(args[0])));
    JsonNode ext = response.path("extensions");
    JsonNode node = ext.path("apolloQueryPlan").path("object").path("node");
    if (node.isMissingNode()) node = ext.path("queryPlan").path("node");
    List<SortedSet<String>> steps = node.isMissingNode() || node.isNull() ? List.of() : steps(node);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("steps", steps);
    out.put("fetches", fetches);
    System.out.println(json.writeValueAsString(out));
  }

  static List<SortedSet<String>> steps(JsonNode n) {
    String kind = n.path("kind").asString();
    switch (kind) {
      case "Fetch", "Subscription" -> {
        fetches++;
        SortedSet<String> s = new TreeSet<>();
        s.add(n.path("serviceName").asString());
        return new ArrayList<>(List.of(s));
      }
      case "Flatten" -> { return steps(n.path("node")); }
      case "Sequence" -> {
        List<SortedSet<String>> out = new ArrayList<>();
        for (JsonNode c : n.path("nodes")) out.addAll(steps(c));
        return out;
      }
      case "Parallel" -> {
        List<SortedSet<String>> out = new ArrayList<>();
        for (JsonNode c : n.path("nodes")) {
          List<SortedSet<String>> branch = steps(c);
          for (int i = 0; i < branch.size(); i++) {
            if (i == out.size()) out.add(new TreeSet<>());
            out.get(i).addAll(branch.get(i));
          }
        }
        return out;
      }
      case "Defer" -> {
        List<SortedSet<String>> out = new ArrayList<>(n.path("primary").has("node") ? steps(n.path("primary").path("node")) : List.of());
        for (JsonNode d : n.path("deferred")) if (d.has("node")) out.addAll(steps(d.path("node")));
        return out;
      }
      default -> throw new IllegalArgumentException("unknown plan node kind: " + kind);
    }
  }
}
