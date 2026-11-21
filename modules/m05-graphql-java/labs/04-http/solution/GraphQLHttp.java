import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.language.OperationDefinition;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * GraphQL over HTTP (September 2026 edition), with no web framework: an HTTP request in, a status,
 * headers and body out. Server.java (JDK HttpServer) and javalin/JavalinServer.java both call handle().
 */
final class GraphQLHttp {
    static final String GQL_JSON = "application/graphql-response+json";
    static final String JSON = "application/json";
    static final JsonMapper MAPPER = JsonMapper.builder().build();

    record Response(int status, Map<String, String> headers, String body) {}

    private final GraphQL graphQL;

    GraphQLHttp(GraphQL graphQL) { this.graphQL = graphQL; }

    /** headers: lower-case names. rawQuery: the URL's query component, undecoded, or null. */
    Response handle(String method, String rawQuery, Map<String, String> headers, byte[] body) {
        if (!method.equals("GET") && !method.equals("POST"))
            return plain(405, "Method not allowed", "Allow", "GET, POST");

        // Content negotiation first: which media type would we answer in?
        String accept = headers.getOrDefault("accept", "*/*");
        String type = negotiate(accept);
        if (type == null) return plain(406, "Supported: " + GQL_JSON + ", " + JSON);

        Map<String, Object> params;
        if (method.equals("POST")) {
            String ct = headers.getOrDefault("content-type", "");
            if (!ct.split(";")[0].trim().equalsIgnoreCase(JSON)) return plain(415, "Send Content-Type: application/json");
            try {
                Object parsed = MAPPER.readValue(body, Object.class);
                if (!(parsed instanceof Map<?, ?> m)) return requestError(422, type, "The body must be a JSON object");
                @SuppressWarnings("unchecked") Map<String, Object> mp = (Map<String, Object>) m;
                params = mp;
            } catch (JacksonException e) {
                return plain(400, "The body is not valid JSON");          // spec: 400, and not our media type
            }
        } else {
            params = new HashMap<>();
            Map<String, String> q = decodeQuery(rawQuery);
            params.putAll(q);
            for (String k : List.of("variables", "extensions")) {
                String v = q.get(k);
                if (v == null || v.isEmpty()) { params.remove(k); continue; }
                try { params.put(k, MAPPER.readValue(v, Object.class)); }
                catch (JacksonException e) { return plain(400, k + " is not valid JSON"); }
            }
            if ("".equals(params.get("operationName"))) params.remove("operationName");
        }

        // Well-formed GraphQL-over-HTTP request? Wrong types or a missing query: 422.
        Object query = params.get("query"), op = params.get("operationName"), vars = params.get("variables"),
               ext = params.get("extensions");
        if (!(query instanceof String qs)) return requestError(422, type, "query must be a string");
        if (op != null && !(op instanceof String)) return requestError(422, type, "operationName must be a string");
        if (vars != null && !(vars instanceof Map)) return requestError(422, type, "variables must be an object");
        if (ext != null && !(ext instanceof Map)) return requestError(422, type, "extensions must be an object");

        // A GET must not run a mutation: find the operation before executing.
        if (method.equals("GET")) {
            try {
                var ops = Parser.parse(qs).getDefinitionsOfType(OperationDefinition.class);
                var chosen = ops.stream().filter(o -> op == null ? ops.size() == 1 : op.equals(o.getName())).findFirst();
                if (chosen.isPresent() && chosen.get().getOperation() == OperationDefinition.Operation.MUTATION)
                    return plain(405, "Mutations need POST", "Allow", "POST");
            } catch (InvalidSyntaxException e) { /* execution reports it as a request error below */ }
        }

        @SuppressWarnings("unchecked") Map<String, Object> variables = vars == null ? Map.of() : (Map<String, Object>) vars;
        @SuppressWarnings("unchecked") Map<String, Object> extensions = ext == null ? Map.of() : (Map<String, Object>) ext;
        var input = ExecutionInput.newExecutionInput(qs).operationName((String) op).variables(variables).extensions(extensions);
        String viewer = headers.get("x-shelfie-user");
        if (viewer != null) input.graphQLContext(Map.of("viewer", viewer));
        Map<String, Object> result = graphQL.execute(input.build()).toSpecification();
        return new Response(status(result, qs, type), Map.of("Content-Type", type + "; charset=utf-8"), MAPPER.writeValueAsString(result));
    }

    /** The highest-priority type we support; application/json for legacy clients; null when nothing fits. */
    static String negotiate(String accept) {
        String best = null; double bestQ = 0;
        for (String part : accept.split(",")) {
            String[] bits = part.trim().split(";");
            String t = bits[0].trim().toLowerCase();
            double q = 1;
            for (int i = 1; i < bits.length; i++)
                if (bits[i].trim().startsWith("q=")) q = Double.parseDouble(bits[i].trim().substring(2));
            String mine = switch (t) {
                case GQL_JSON, "application/*", "*/*" -> GQL_JSON;
                case JSON -> JSON;
                default -> null;
            };
            if (mine != null && q > bestQ) { best = mine; bestQ = q; }
        }
        return best;
    }

    /** Spec status codes. No data: request error (400 for syntax, 422 otherwise). Data and errors: 294. */
    static int status(Map<String, Object> result, String query, String type) {
        if (!result.containsKey("data")) {
            try { Parser.parse(query); } catch (InvalidSyntaxException e) { return 400; }
            return 422;
        }
        if (!result.containsKey("errors")) return 200;
        // 294 only with our own media type: legacy application/json clients get 200.
        return type.equals(GQL_JSON) ? 294 : 200;
    }

    private Response requestError(int status, String type, String message) {
        var body = Map.of("errors", List.of(Map.of("message", message)));
        return new Response(status, Map.of("Content-Type", type + "; charset=utf-8"), MAPPER.writeValueAsString(body));
    }

    /** Errors that are not GraphQL responses: plain text, never application/graphql-response+json. */
    private static Response plain(int status, String text, String... header) {
        var h = new LinkedHashMap<String, String>();
        h.put("Content-Type", "text/plain; charset=utf-8");
        for (int i = 0; i + 1 < header.length; i += 2) h.put(header[i], header[i + 1]);
        return new Response(status, h, text);
    }

    static Map<String, String> decodeQuery(String raw) {
        var out = new LinkedHashMap<String, String>();
        if (raw == null || raw.isEmpty()) return out;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String k = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
            String v = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            out.put(k, v);
        }
        return out;
    }
}
