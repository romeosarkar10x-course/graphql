import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** Prints an ExecutionResult the way the HTTP layer would: {"data": ..., "errors": [...]}. */
final class Json {
    private Json() {}
    static final JsonMapper MAPPER = JsonMapper.builder().build();

    static String of(Object value) { return MAPPER.writeValueAsString(value); }

    @SuppressWarnings("unchecked")
    static Map<String, Object> read(String json) { return MAPPER.readValue(json, Map.class); }
}
