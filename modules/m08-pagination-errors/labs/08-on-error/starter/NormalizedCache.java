import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * A tiny normalized client cache, like Apollo's or Relay's store: every object with an id becomes one
 * record, and references between objects are stored as {"__ref": id}.
 * TODO (lab 8.8 step 3): make it error-aware. A field whose path has an error must not be written, so a
 * null caused by an error never overwrites a good cached value.
 */
public class NormalizedCache {
    private final Map<String, Map<String, Object>> records = new LinkedHashMap<>();

    public Map<String, Map<String, Object>> records() { return records; }

    /** Writes one response. Returns how many fields it stored. */
    public int write(JsonNode response) {
        Set<String> errorPaths = new HashSet<>();   // TODO: fill from response.errors[].path
        JsonNode data = response.path("data");
        if (!data.isObject()) return 0;            // data: null, or no data at all: nothing to keep
        int stored = 0;
        for (var entry : data.properties()) stored += walk(entry.getValue(), "[\"" + entry.getKey() + "\"", errorPaths);
        return stored;
    }

    /** Stores the object at `path` (a JSON array prefix without its closing bracket) and everything below it. */
    private int walk(JsonNode node, String path, Set<String> errorPaths) {
        if (node.isArray()) {
            int n = 0;
            for (int i = 0; i < node.size(); i++) n += walk(node.get(i), path + "," + i, errorPaths);
            return n;
        }
        if (!node.isObject() || !node.has("id")) return 0;
        var record = records.computeIfAbsent(node.get("id").asString(), k -> new LinkedHashMap<>());
        int stored = 0;
        for (var field : node.properties()) {
            String fieldPath = path + ",\"" + field.getKey() + "\"";
            JsonNode value = field.getValue();
            if (value.isObject() && value.has("id")) {
                record.put(field.getKey(), Map.of("__ref", value.get("id").asString()));
                stored += 1 + walk(value, fieldPath, errorPaths);
            } else if (value.isArray()) {
                var refs = new java.util.ArrayList<Object>();
                for (var item : value) refs.add(item.isObject() && item.has("id") ? Map.of("__ref", item.get("id").asString()) : item);
                record.put(field.getKey(), List.copyOf(refs));
                stored += 1 + walk(value, fieldPath, errorPaths);
            } else {
                record.put(field.getKey(), value.isNull() ? null : value);
                stored++;
            }
        }
        return stored;
    }
}
