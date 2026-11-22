import com.sun.net.httpserver.HttpServer;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 13.4's mock server: any SDL file, no resolvers, data from MockWiring. Do not edit (the tests run the
 * course's copy, modules/m13-testing/contract/mock/MockServer.java); write MockWiring.java next to it.
 *
 *   bin/course java MockServer.java <schema.graphqls> [port=4000] [seed=13]
 *   POST /graphql {"query", "variables", "operationName"}  ->  application/json
 */
public class MockServer {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String sdl = Files.readString(Path.of(args[0]));
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 4000;
        long seed = args.length > 2 ? Long.parseLong(args[2]) : 13;
        var schema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl),
            RuntimeWiring.newRuntimeWiring().wiringFactory(new MockWiring(seed)).build());
        var graphql = GraphQL.newGraphQL(schema).build();
        var json = JsonMapper.builder().build();
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/graphql", ex -> {
            try (ex) {
                if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
                Map<String, Object> body = json.readValue(ex.getRequestBody(), Map.class);
                @SuppressWarnings("unchecked") var vars = (Map<String, Object>) body.getOrDefault("variables", Map.of());
                var input = ExecutionInput.newExecutionInput().query((String) body.get("query"))
                    .operationName((String) body.get("operationName")).variables(vars == null ? Map.of() : vars).build();
                byte[] out = json.writeValueAsBytes(graphql.execute(input).toSpecification());
                ex.getResponseHeaders().set("Content-Type", "application/json");
                ex.sendResponseHeaders(200, out.length);
                ex.getResponseBody().write(out);
            } catch (Exception e) {
                byte[] out = ("{\"errors\":[{\"message\":" + json.writeValueAsString(String.valueOf(e.getMessage())) + "}]}").getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "application/json");
                ex.sendResponseHeaders(400, out.length);
                ex.getResponseBody().write(out);
            }
        });
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("mock server on :" + port + " for " + args[0] + " (seed " + seed + ")");
    }
}
