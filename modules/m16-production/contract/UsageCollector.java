import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A stand-in for a schema registry's usage endpoint (Module 16.2): accepts Hive usage reports, version 2, and keeps
 * them so you can ask what was used. Not Hive: it checks the parts of Hive's report schema
 * (packages/services/usage/usage-report-v2.schema.json in graphql-hive/console) that the labs rely on.
 *
 *   bin/course java modules/m16-production/contract/UsageCollector.java [port]      (default 4200)
 *
 *   POST /usage        a report; needs X-Usage-API-Version: 2 and Authorization: Bearer course-usage-token.
 *                      200 {"accepted": n} or 400 {"errors": [...]} (nothing stored) or 401
 *   GET  /_reports     every accepted report, as sent: [report, ...]
 *   GET  /_stats       {"reports": n, "operations": n, "rejected": n}       POST /_reset   forget everything
 */
public class UsageCollector {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final List<JsonNode> REPORTS = new ArrayList<>();
    static int operations, rejected;
    static final Set<String> TOP = Set.of("size", "map", "operations", "subscriptionOperations");
    static final Set<String> RECORD = Set.of("operation", "operationName", "fields");
    static final Set<String> OPERATION = Set.of("timestamp", "operationMapKey", "execution", "metadata", "persistedDocumentHash");
    static final Set<String> EXECUTION = Set.of("ok", "duration", "errorsTotal", "fetches", "gatewayErrors");

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 4200;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 128);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/usage", ex -> {
            try (ex) {
                if (!"2".equals(ex.getRequestHeaders().getFirst("X-Usage-API-Version"))) {
                    send(ex, 400, Map.of("errors", List.of("X-Usage-API-Version must be 2")));
                    return;
                }
                if (!"Bearer course-usage-token".equals(ex.getRequestHeaders().getFirst("Authorization"))) {
                    send(ex, 401, Map.of("errors", List.of("Authorization: Bearer <token> missing or wrong")));
                    return;
                }
                JsonNode report;
                try {
                    report = JSON.readTree(ex.getRequestBody());
                } catch (Exception e) {
                    send(ex, 400, Map.of("errors", List.of("not JSON: " + e.getMessage())));
                    return;
                }
                List<String> errors = check(report);
                synchronized (REPORTS) {
                    if (!errors.isEmpty()) {
                        rejected++;
                        send(ex, 400, Map.of("errors", errors.subList(0, Math.min(10, errors.size()))));
                        return;
                    }
                    REPORTS.add(report);
                    operations += report.path("operations").size();
                }
                send(ex, 200, Map.of("accepted", report.path("operations").size()));
            }
        });
        server.createContext("/_reports", ex -> { try (ex) { synchronized (REPORTS) { send(ex, 200, REPORTS); } } });
        server.createContext("/_stats", ex -> { try (ex) { synchronized (REPORTS) {
            send(ex, 200, Map.of("reports", REPORTS.size(), "operations", operations, "rejected", rejected)); } } });
        server.createContext("/_reset", ex -> { try (ex) { synchronized (REPORTS) {
            REPORTS.clear(); operations = 0; rejected = 0; send(ex, 200, Map.of("reset", true)); } } });
        server.start();
        System.out.println("usage collector on :" + port);
    }

    static List<String> check(JsonNode r) {
        List<String> e = new ArrayList<>();
        if (!r.isObject()) return List.of("a report is a JSON object");
        r.propertyNames().forEach(k -> { if (!TOP.contains(k)) e.add("unknown top-level property " + k); });
        if (!r.path("size").isIntegralNumber() || r.path("size").asInt() < 1) e.add("size must be an integer >= 1");
        if (!r.path("map").isObject()) { e.add("map must be an object"); return e; }
        JsonNode map = r.path("map");
        for (String key : map.propertyNames()) {
            JsonNode rec = map.path(key);
            rec.propertyNames().forEach(k -> { if (!RECORD.contains(k)) e.add("map." + key + ": unknown property " + k); });
            if (!rec.path("operation").isString()) e.add("map." + key + ".operation must be a string");
            if (!rec.path("fields").isArray() || rec.path("fields").isEmpty()) e.add("map." + key + ".fields must be a non-empty array");
            else rec.path("fields").forEach(f -> { if (!f.isString()) e.add("map." + key + ".fields must hold strings"); });
            if (rec.has("operationName") && !rec.path("operationName").isString() && !rec.path("operationName").isNull())
                e.add("map." + key + ".operationName must be a string or null");
        }
        JsonNode ops = r.path("operations");
        if (!ops.isArray()) { e.add("operations must be an array"); return e; }
        if (ops.size() != r.path("size").asInt()) e.add("size (" + r.path("size").asInt() + ") must equal the number of operations (" + ops.size() + ")");
        int i = 0;
        for (JsonNode op : ops) {
            String at = "operations[" + i++ + "]";
            op.propertyNames().forEach(k -> { if (!OPERATION.contains(k)) e.add(at + ": unknown property " + k); });
            if (!op.path("timestamp").isIntegralNumber()) e.add(at + ".timestamp must be an integer (ms since the epoch)");
            else if (op.path("timestamp").asLong() < 1_000_000_000_000L) e.add(at + ".timestamp looks like seconds, not milliseconds");
            if (!map.has(op.path("operationMapKey").asString(""))) e.add(at + ".operationMapKey is not a key of map");
            JsonNode x = op.path("execution");
            x.propertyNames().forEach(k -> { if (!EXECUTION.contains(k)) e.add(at + ".execution: unknown property " + k); });
            if (!x.path("ok").isBoolean()) e.add(at + ".execution.ok must be a boolean");
            if (!x.path("duration").isIntegralNumber() || x.path("duration").asLong() < 0) e.add(at + ".execution.duration must be an integer >= 0 (nanoseconds)");
            if (!x.path("errorsTotal").isIntegralNumber()) e.add(at + ".execution.errorsTotal must be an integer");
            JsonNode client = op.path("metadata").path("client");
            if (op.has("metadata") && !client.isMissingNode() && !client.isNull()
                    && (!client.path("name").isString() || !client.path("version").isString()))
                e.add(at + ".metadata.client needs a string name and version");
        }
        return e;
    }

    static void send(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = JSON.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
    }
}
