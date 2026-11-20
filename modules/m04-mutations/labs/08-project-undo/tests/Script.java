import graphql.ExecutionInput;
import graphql.GraphQL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs a test script against a fresh Undo.graphQL(): each step is {as, doc, vars, save}. "save" names a
 * token to keep ({"t1": "renameClub"} keeps data.renameClub.undoToken); a variable "$t1" uses it.
 * Prints each response with undo tokens masked, so output is deterministic.
 */
public class Script {
    static final JsonMapper JSON = JsonMapper.builder().build();

    @SuppressWarnings("unchecked")
    static Object mask(Object v) {
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new java.util.LinkedHashMap<>();
            m.forEach((k, x) -> out.put((String) k, k.equals("undoToken") && x != null ? "<token>" : mask(x)));
            return out;
        }
        if (v instanceof List<?> l) return l.stream().map(Script::mask).toList();
        return v;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        GraphQL g = Undo.graphQL();
        Map<String, String> saved = new HashMap<>();
        int n = 0;
        for (Object o : JSON.readValue(Files.readString(Path.of(args[0])), List.class)) {
            Map<String, Object> step = (Map<String, Object>) o;
            Map<String, Object> vars = new HashMap<>();
            ((Map<String, Object>) step.getOrDefault("vars", Map.of())).forEach((k, v) ->
                    vars.put(k, v instanceof String s && s.startsWith("$") ? saved.getOrDefault(s.substring(1), "missing-" + s) : v));
            var r = g.execute(ExecutionInput.newExecutionInput((String) step.get("doc")).variables(vars)
                    .graphQLContext(Map.of("viewer", step.getOrDefault("as", "u1"))).build());
            var spec = r.toSpecification();
            ((Map<String, String>) step.getOrDefault("save", Map.of())).forEach((name, field) -> {
                Object data = spec.get("data");
                Object payload = data instanceof Map<?, ?> d ? d.get(field) : null;
                Object token = payload instanceof Map<?, ?> p ? p.get("undoToken") : null;
                if (token != null) saved.put(name, token.toString());
            });
            System.out.println(String.format("%02d", ++n) + " " + JSON.writeValueAsString(mask(spec)));
        }
    }
}
