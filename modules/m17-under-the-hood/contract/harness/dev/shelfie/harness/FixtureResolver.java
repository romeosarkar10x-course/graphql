package dev.shelfie.harness;

import dev.shelfie.mini.Resolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Resolvers for conformance/fixture.graphqls. Values come from the test's data, a map per object; a value
 * {"$throw": "message"} makes the resolver throw; {"$sleep": ms, "value": v} returns v after ms. echo returns its coerced arguments as JSON with sorted keys;
 * Mutation.step sleeps, appends its name to the log and returns the log so far.
 */
public final class FixtureResolver implements Resolver {
    static final ObjectMapper JSON = JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
    /** How many slow resolvers ran on a virtual thread. */
    public final java.util.concurrent.atomic.AtomicInteger virtual = new java.util.concurrent.atomic.AtomicInteger();
    private final List<String> log = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Object resolve(String type, String field, Object source, Map<String, Object> args) throws Exception {
        if (type.equals("Mutation") && field.equals("step")) {
            Thread.sleep((Integer) args.get("ms"));
            log.add((String) args.get("name"));
            return List.copyOf(log);
        }
        if (type.equals("Query") && field.equals("echo")) return JSON.writeValueAsString(args);
        Object value = source instanceof Map<?, ?> m ? m.get(field) : null;
        if (value instanceof Map<?, ?> m && m.containsKey("$throw")) throw new RuntimeException(String.valueOf(m.get("$throw")));
        if (value instanceof Map<?, ?> m && m.containsKey("$sleep")) {
            if (Thread.currentThread().isVirtual()) virtual.incrementAndGet();
            Thread.sleep(((Number) m.get("$sleep")).longValue());
            return m.get("value");
        }
        return value;
    }
}
