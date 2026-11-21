package dev.shelfie.perf;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** A minimal GraphQL client for tests: POST a query to the app under test, get the response as a Map. */
public final class Gql {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Gql() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> post(int port, String query) {
        try {
            var body = JSON.writeValueAsString(Map.of("query", query));
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/graphql"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            return JSON.readValue(HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body(), Map.class);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
