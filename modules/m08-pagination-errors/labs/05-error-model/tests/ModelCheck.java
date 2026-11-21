import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 8.5 harness. 1) Each throw is where the README puts it (the error's path is its origin).
 * 2) Every error has the shape the spec requires. 3) predictions.json is complete; how many of the
 * predictions match is reported, not graded: a wrong prediction is the point of the exercise.
 */
public class ModelCheck {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static int fails = 0;
    static void check(boolean ok, String what) { System.out.println((ok ? "ok    " : "FAIL  ") + what); if (!ok) fails++; }

    static final Map<String, List<Object>> ORIGIN = Map.of(
        "E1", List.of("hello"),
        "E2", List.of("club", "owner"),
        "E3", List.of("club", "members", 2, "name"),
        "E4", List.of("club", "currentBook", "title"),
        "E5", List.of("club", "currentBook", "rating"),
        "E6", List.of("a", "owner"),
        "E7", List.of("club", "reviews", 1, "body"));

    public static void main(String[] args) throws Exception {
        var actual = new java.util.LinkedHashMap<String, JsonNode>();
        for (String name : Depths.CASES.keySet()) actual.put(name, JSON.valueToTree(Depths.run(name)));

        for (var e : actual.entrySet()) {
            String name = e.getKey();
            JsonNode res = e.getValue(), errors = res.path("errors");
            boolean shaped = errors.isArray() && !errors.isEmpty();
            for (var err : errors) shaped &= err.path("message").isString() && err.path("locations").isArray();
            if (name.startsWith("R")) {
                check(!res.has("data") && shaped, name + ": a request error: no data key, errors with message and locations, no path");
                for (var err : errors) if (err.has("path")) check(false, name + ": request errors have no path");
            } else {
                var paths = new ArrayList<JsonNode>();
                for (var err : errors) paths.add(err.path("path"));
                check(res.has("data") && shaped && paths.size() == 1 && paths.get(0).equals(JSON.valueToTree(ORIGIN.get(name))),
                    name + ": one execution error, raised at " + ORIGIN.get(name));
            }
        }

        var file = Path.of("predictions.json");
        JsonNode predictions = Files.exists(file) ? JSON.readTree(Files.readString(file)) : JSON.createObjectNode();
        int complete = 0, right = 0;
        var wrong = new ArrayList<String>();
        for (var e : actual.entrySet()) {
            JsonNode p = predictions.path(e.getKey());
            boolean filled = p.has("data") && !p.path("data").asString("").equals("TODO") && p.path("errorPaths").isArray();
            if (!filled) continue;
            complete++;
            JsonNode res = e.getValue();
            JsonNode paths = JSON.createArrayNode();
            for (var err : res.path("errors")) ((tools.jackson.databind.node.ArrayNode) paths).add(err.has("path") ? err.get("path") : JSON.createArrayNode());
            boolean dataOk = p.path("data").asString("").equals("absent") ? !res.has("data") : res.has("data") && p.get("data").equals(res.get("data"));
            boolean pathsOk = p.get("errorPaths").equals(paths)
                || (e.getKey().startsWith("R") && p.get("errorPaths").isEmpty() && paths.size() == 1);
            if (dataOk && pathsOk) right++; else wrong.add(e.getKey());
        }
        check(complete == actual.size(), "predictions.json has a prediction for all " + actual.size() + " cases (" + complete + " filled in)");
        System.out.println("info  predictions: " + right + " of " + actual.size() + " match" + (wrong.isEmpty() ? "" : "; wrong: " + String.join(", ", wrong) + " (explain them in notes.md)"));
        System.exit(fails == 0 ? 0 : 1);
    }
}
