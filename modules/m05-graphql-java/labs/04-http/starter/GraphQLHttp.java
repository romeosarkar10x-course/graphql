import graphql.ExecutionInput;
import graphql.GraphQL;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * GraphQL over HTTP with no web framework: an HTTP request in, a status, headers and body out.
 * Server.java (JDK HttpServer) calls handle(); so can a Javalin adapter (bonus).
 *
 * TODO: this is the naive version most tutorials stop at. Run the audit (README) and make it pass:
 * methods, content negotiation, GET, request validation and the spec's status codes.
 */
final class GraphQLHttp {
    static final JsonMapper MAPPER = JsonMapper.builder().build();

    record Response(int status, Map<String, String> headers, String body) {}

    private final GraphQL graphQL;

    GraphQLHttp(GraphQL graphQL) { this.graphQL = graphQL; }

    /** headers: lower-case names. rawQuery: the URL's query component, undecoded, or null. */
    @SuppressWarnings("unchecked")
    Response handle(String method, String rawQuery, Map<String, String> headers, byte[] body) {
        Map<String, Object> params = MAPPER.readValue(body, Map.class);
        var input = ExecutionInput.newExecutionInput((String) params.get("query"))
            .operationName((String) params.get("operationName"))
            .variables(params.get("variables") == null ? Map.of() : (Map<String, Object>) params.get("variables"));
        String viewer = headers.get("x-shelfie-user");
        if (viewer != null) input.graphQLContext(Map.of("viewer", viewer));
        Map<String, Object> result = graphQL.execute(input.build()).toSpecification();
        return new Response(200, Map.of("Content-Type", "application/json"), MAPPER.writeValueAsString(result));
    }
}
