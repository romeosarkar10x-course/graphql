package dev.shelfie.mock;

import com.sun.net.httpserver.HttpServer;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.MockedWiringFactory;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * A mock GraphQL server for any SDL file. Today: graphql-java's MockedWiringFactory, which answers null
 * everywhere, and only the schema argument. See the README for what the finished jar does:
 *
 *   java -jar mock-server.jar <schema.graphqls> [--port 4000] [--seed 13] [--mocks <dir>]
 *
 * TODO realistic deterministic data (start from your lab 13.4 MockWiring), the options, MockSpec's @mock.
 */
public final class Main {

    public static void main(String[] args) throws IOException {
        var schema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(Files.readString(Path.of(args[0]))),
            RuntimeWiring.newRuntimeWiring().wiringFactory(new MockedWiringFactory()).build());
        var graphql = GraphQL.newGraphQL(schema).build();
        var json = JsonMapper.builder().build();
        var server = HttpServer.create(new InetSocketAddress(4000), 0);
        server.createContext("/graphql", ex -> {
            try (ex) {
                @SuppressWarnings("unchecked") Map<String, Object> body = json.readValue(ex.getRequestBody(), Map.class);
                var result = graphql.execute(ExecutionInput.newExecutionInput().query((String) body.get("query")).build());
                byte[] out = json.writeValueAsBytes(result.toSpecification());
                ex.getResponseHeaders().set("Content-Type", "application/json");
                ex.sendResponseHeaders(200, out.length);
                ex.getResponseBody().write(out);
            }
        });
        server.start();
    }
}
