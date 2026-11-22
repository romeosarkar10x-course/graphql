package dev.shelfie.nadel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import graphql.nadel.Nadel;
import graphql.nadel.NadelExecutionInput;
import graphql.nadel.ServiceExecution;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import tools.jackson.databind.json.JsonMapper;

/**
 * A Nadel gateway over two plain GraphQL services. The exposed (overall) schema is the .nadel files; each service's
 * own schema is in underlying/. Nadel splits each request per service and calls them through ServiceExecution.
 *
 *   java -jar target/shelfie-nadel.jar [--port=4000] [--catalog=http://localhost:4101/graphql] [--reviews=...] [--schemas=src/main/resources]
 */
public class Gateway {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws IOException {
        Map<String, String> opts = new HashMap<>(Map.of("port", "4000",
                "catalog", "http://localhost:4101/graphql", "reviews", "http://localhost:4102/graphql"));
        for (String a : args) if (a.startsWith("--") && a.contains("=")) opts.put(a.substring(2, a.indexOf('=')), a.substring(a.indexOf('=') + 1));

        dir = opts.get("schemas");
        Nadel nadel = Nadel.newNadel()
                .overallSchema("catalog", resource("nadel/catalog.nadel"))
                .overallSchema("reviews", resource("nadel/reviews.nadel"))
                .underlyingSchema("catalog", resource("underlying/catalog.graphqls"))
                .underlyingSchema("reviews", resource("underlying/reviews.graphqls"))
                .serviceExecutionFactory(service -> forward(opts.get(service)))
                .build();

        HttpServer server = HttpServer.create(new InetSocketAddress(Integer.parseInt(opts.get("port"))), 64);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/graphql", ex -> {
            try {
                Map<String, Object> body = JSON.readValue(ex.getRequestBody().readAllBytes(), Map.class);
                @SuppressWarnings("unchecked") Map<String, Object> vars = (Map<String, Object>) body.get("variables");
                var input = NadelExecutionInput.newNadelExecutionInput()
                        .query((String) body.get("query"))
                        .operationName((String) body.get("operationName"))
                        .variables(vars == null ? Map.of() : vars)
                        .build();
                send(ex, nadel.execute(input).join().toSpecification());
            } catch (RuntimeException e) {
                send(ex, Map.of("errors", List.of(Map.of("message", String.valueOf(e.getMessage())))));
            }
        });
        server.start();
        System.out.println("nadel gateway on " + opts.get("port"));
    }

    /** One service call. TODO (lab 15.2): send the document Nadel built for this service to its URL. */
    static ServiceExecution forward(String url) {
        return params -> CompletableFuture.failedFuture(new UnsupportedOperationException("forward() is not written yet"));
    }

    static String dir;

    /** From --schemas=<dir> when given (edit and restart, no rebuild), else from the jar. */
    static String resource(String name) throws IOException {
        if (dir != null) return java.nio.file.Files.readString(java.nio.file.Path.of(dir, name));
        try (InputStream in = Gateway.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) throw new IOException("missing resource " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static void send(HttpExchange ex, Object body) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, bytes.length);
        try (var out = ex.getResponseBody()) { out.write(bytes); }
    }
}
