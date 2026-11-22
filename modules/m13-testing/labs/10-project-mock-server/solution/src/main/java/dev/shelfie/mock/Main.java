package dev.shelfie.mock;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import tools.jackson.databind.json.JsonMapper;

/**
 * A mock GraphQL server for any SDL file: realistic, deterministic fake data with no resolvers, plus MockSpec's
 * named mocks from a directory (RFC MockSpec, strawman: @mock(name:) on the operation picks a response from
 * <mocks>/<OperationName>.json; a missing file is generated from the fake data).
 *
 *   java -jar mock-server.jar <schema.graphqls> [--port 4000] [--seed 13] [--mocks <dir>]
 *   POST /graphql   {"query", "variables", "operationName"}   (GET answers 405)
 */
public final class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) { System.err.println("usage: java -jar mock-server.jar <schema.graphqls> [--port N] [--seed N] [--mocks dir]"); System.exit(2); }
        int port = 4000; long seed = 13; Path mocks = null;
        for (int i = 1; i + 1 < args.length; i += 2)
            switch (args[i]) {
                case "--port" -> port = Integer.parseInt(args[i + 1]);
                case "--seed" -> seed = Long.parseLong(args[i + 1]);
                case "--mocks" -> mocks = Path.of(args[i + 1]);
                default -> { System.err.println("unknown option " + args[i]); System.exit(2); }
            }
        TypeDefinitionRegistry registry = new SchemaParser().parse(Files.readString(Path.of(args[0])));
        if (registry.getDirectiveDefinition("mock").isEmpty())
            registry.merge(new SchemaParser().parse("directive @mock(name: String) on QUERY | MUTATION | SUBSCRIPTION"));
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(registry,
            RuntimeWiring.newRuntimeWiring().wiringFactory(new MockWiring(seed)).build());
        GraphQL graphql = GraphQL.newGraphQL(schema).build();
        MockSpec spec = new MockSpec(schema, mocks);
        JsonMapper json = JsonMapper.builder().build();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/graphql", ex -> {
            try (ex) {
                if (!"POST".equals(ex.getRequestMethod())) {
                    ex.getResponseHeaders().set("Allow", "POST");
                    ex.sendResponseHeaders(405, -1);
                    return;
                }
                Map<String, Object> response;
                try {
                    @SuppressWarnings("unchecked") Map<String, Object> body = json.readValue(ex.getRequestBody(), Map.class);
                    String query = (String) body.get("query");
                    String op = (String) body.get("operationName");
                    @SuppressWarnings("unchecked") Map<String, Object> vars = (Map<String, Object>) body.get("variables");
                    var input = ExecutionInput.newExecutionInput().query(query == null ? "" : query).operationName(op)
                        .variables(vars == null ? Map.of() : vars).build();
                    response = spec.handles(query, op) ? spec.respond(graphql, input) : graphql.execute(input).toSpecification();
                } catch (tools.jackson.core.JacksonException e) {
                    response = Map.of("errors", List.of(Map.of("message", "The body is not JSON: " + e.getOriginalMessage())));
                }
                send(ex, json.writeValueAsBytes(response));
            }
        });
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("mock server on :" + port + " for " + args[0] + " (seed " + seed + (mocks == null ? "" : ", mocks in " + mocks) + ")");
    }

    static void send(HttpExchange ex, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
    }
}
