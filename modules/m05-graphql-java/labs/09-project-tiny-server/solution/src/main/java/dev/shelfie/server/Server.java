package dev.shelfie.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.language.OperationDefinition;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import tools.jackson.databind.json.JsonMapper;

/** Mini-project 5: Shelfie over GraphQL-over-HTTP with the JDK, graphql-java and Jackson. Nothing else. */
public final class Server {
    static final String GQL = "application/graphql-response+json", JSON = "application/json";
    static final JsonMapper MAPPER = JsonMapper.builder().build();
    static GraphQL graphQL;

    public static void main(String[] args) throws IOException {
        var sdl = new String(Server.class.getResourceAsStream("/shelfie.graphqls").readAllBytes(), StandardCharsets.UTF_8);
        graphQL = GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), wiring()))
            .defaultDataFetcherExceptionHandler(p -> {   // mask everything: the log gets the details
                System.err.println("field error at " + p.getPath() + ": " + p.getException());
                return CompletableFuture.completedFuture(DataFetcherExceptionHandlerResult.newResult(GraphqlErrorBuilder
                    .newError(p.getDataFetchingEnvironment()).message("Internal error").build()).build());
            }).build();
        var server = HttpServer.create(new InetSocketAddress(args.length > 0 ? Integer.parseInt(args[0]) : 4000), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/graphql", ex -> { try (ex) { serve(ex); } });
        server.start();
    }

    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
            .type("Query", t -> t.dataFetcher("hello", e -> "Hello from Shelfie")
                .dataFetcher("club", e -> Data.club(e.getArgument("id")).orElse(null))
                .dataFetcher("clubs", e -> Data.CLUBS)
                .dataFetcher("user", e -> Data.user(e.getArgument("id")).orElse(null))
                .dataFetcher("book", e -> Data.book(e.getArgument("id")).orElse(null))
                .dataFetcher("books", e -> Data.BOOKS.stream().filter(b -> e.getArgument("titleContains") == null
                    || b.title().toLowerCase().contains(e.<String>getArgument("titleContains").toLowerCase())).limit(first(e, "first")).toList())
                .dataFetcher("viewer", e -> Data.user(e.getGraphQlContext().getOrDefault("viewer", "")).orElse(null))
                .dataFetcher("boom", e -> { throw new IllegalStateException("boom: secret internals"); }))
            .type("Mutation", t -> t.dataFetcher("createReview", Server::createReview))
            .type("Club", t -> t.dataFetcher("owner", e -> Data.user(e.<Data.Club>getSource().ownerId()).orElseThrow())
                .dataFetcher("members", e -> e.<Data.Club>getSource().memberIds().stream().limit(first(e, "first")).map(id -> Data.user(id).orElseThrow()).toList())
                .dataFetcher("memberCount", e -> e.<Data.Club>getSource().memberIds().size())
                .dataFetcher("currentBook", e -> Data.book(e.<Data.Club>getSource().currentBookId()).orElse(null))
                .dataFetcher("meetups", e -> Data.meetupsOf(e.<Data.Club>getSource().id(), e.getArgument("upcomingOnly"))))
            .type("User", t -> t.dataFetcher("clubs", e -> Data.clubsOf(e.<Data.User>getSource().id()))
                .dataFetcher("reviews", e -> Data.reviewsBy(e.<Data.User>getSource().id())))
            .type("Book", t -> t.dataFetcher("averageRating", e -> Data.averageRating(e.<Data.Book>getSource().id()))
                .dataFetcher("reviews", e -> Data.reviewsOf(e.<Data.Book>getSource().id()).stream().limit(first(e, "last")).toList()))
            .type("Review", t -> t.dataFetcher("author", e -> Data.user(e.<Data.Review>getSource().authorId()).orElseThrow())
                .dataFetcher("book", e -> Data.book(e.<Data.Review>getSource().bookId()).orElseThrow()))
            .type("Meetup", t -> t.dataFetcher("club", e -> Data.club(e.<Data.Meetup>getSource().clubId()).orElseThrow()))
            .build();
    }

    static long first(DataFetchingEnvironment e, String arg) { return Math.max(0, e.<Integer>getArgument(arg)); }

    static Map<String, Object> createReview(DataFetchingEnvironment e) {
        Map<String, Object> in = e.getArgument("input");
        String viewer = e.getGraphQlContext().get("viewer"), bookId = (String) in.get("bookId");
        int rating = (Integer) in.get("rating");
        if (viewer == null) return problem(null, "Sign in to review (X-Shelfie-User header)");
        if (Data.book(bookId).isEmpty()) return problem(List.of("input", "bookId"), "No book " + bookId);
        if (rating < 1 || rating > 5) return problem(List.of("input", "rating"), "Rating must be 1 to 5");
        return Map.of("review", Data.addReview(bookId, viewer, rating, (String) in.get("body")), "userErrors", List.of());
    }

    static Map<String, Object> problem(List<String> field, String message) {
        var error = new HashMap<String, Object>(Map.of("message", message));
        error.put("field", field);
        var payload = new HashMap<String, Object>(Map.of("userErrors", List.of(error)));
        payload.put("review", null);
        return payload;
    }

    static void serve(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod(), accept = ex.getRequestHeaders().getFirst("Accept");
        if (!method.equals("GET") && !method.equals("POST")) { send(ex, 405, "text/plain", "Use GET or POST", "GET, POST"); return; }
        String type = accept == null || accept.isBlank() ? GQL : negotiate(accept);
        if (type == null) { send(ex, 406, "text/plain", "Accept " + GQL + " or " + JSON, null); return; }
        Map<String, Object> req = new HashMap<>();
        try {
            if (method.equals("POST")) {
                String ct = String.valueOf(ex.getRequestHeaders().getFirst("Content-Type")).split(";")[0].trim();
                if (!ct.equalsIgnoreCase(JSON)) { send(ex, 415, "text/plain", "Send application/json", null); return; }
                if (!(MAPPER.readValue(ex.getRequestBody(), Object.class) instanceof Map<?, ?> m)) { reject(ex, type, "Send a JSON object"); return; }
                m.forEach((k, v) -> req.put((String) k, v));
            } else if (ex.getRequestURI().getRawQuery() != null) {
                for (String pair : ex.getRequestURI().getRawQuery().split("&")) {
                    String[] kv = pair.split("=", 2);
                    String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                    if (!v.isEmpty()) req.put(kv[0], kv[0].equals("variables") || kv[0].equals("extensions") ? MAPPER.readValue(v, Object.class) : v);
                }
            }
        } catch (tools.jackson.core.JacksonException e) { send(ex, 400, "text/plain", "Malformed JSON", null); return; }
        if (!(req.get("query") instanceof String query) || !(req.getOrDefault("operationName", "") instanceof String op)
            || !(req.getOrDefault("variables", Map.of()) instanceof Map<?, ?> vars) || !(req.getOrDefault("extensions", Map.of()) instanceof Map)) {
            reject(ex, type, "Not a well-formed GraphQL-over-HTTP request"); return;
        }
        if (method.equals("GET") && isMutation(query, op)) { send(ex, 405, "text/plain", "Mutations need POST", "POST"); return; }
        var input = ExecutionInput.newExecutionInput(query).operationName(op.isEmpty() ? null : op).variables(cast(vars));
        String viewer = ex.getRequestHeaders().getFirst("X-Shelfie-User");
        if (viewer != null) input.graphQLContext(Map.of("viewer", viewer));
        Map<String, Object> result = graphQL.execute(input.build()).toSpecification();
        int status = !result.containsKey("data") ? (parses(query) ? 422 : 400) : !result.containsKey("errors") || type.equals(JSON) ? 200 : 294;
        send(ex, status, type, MAPPER.writeValueAsString(result), null);
    }

    static String negotiate(String accept) {
        String best = null; double bestQ = 0;
        for (String part : accept.split(",")) {
            String[] p = part.trim().split(";");
            double q = p.length > 1 && p[1].trim().startsWith("q=") ? Double.parseDouble(p[1].trim().substring(2)) : 1;
            String t = switch (p[0].trim()) { case GQL, "application/*", "*/*" -> GQL; case JSON -> JSON; default -> null; };
            if (t != null && q > bestQ) { best = t; bestQ = q; }
        }
        return best;
    }

    static boolean parses(String q) { try { Parser.parse(q); return true; } catch (InvalidSyntaxException e) { return false; } }

    static boolean isMutation(String query, String op) {
        if (!parses(query)) return false;
        var ops = Parser.parse(query).getDefinitionsOfType(OperationDefinition.class);
        return ops.stream().filter(o -> op.isEmpty() ? ops.size() == 1 : op.equals(o.getName()))
            .anyMatch(o -> o.getOperation() == OperationDefinition.Operation.MUTATION);
    }

    @SuppressWarnings("unchecked") static Map<String, Object> cast(Map<?, ?> m) { return (Map<String, Object>) m; }

    static void reject(HttpExchange ex, String type, String message) throws IOException {
        send(ex, 422, type, MAPPER.writeValueAsString(Map.of("errors", List.of(Map.of("message", message)))), null);
    }

    static void send(HttpExchange ex, int status, String type, String body, String allow) throws IOException {
        byte[] out = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        if (allow != null) ex.getResponseHeaders().set("Allow", allow);
        ex.sendResponseHeaders(status, out.length);
        ex.getResponseBody().write(out);
    }
}
