package dev.shelfie.digest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A downstream caller of Shelfie (Module 13.4): one user's reviews as a weekly digest. Tests stand in for
 * Shelfie with WireMock, so they run with no server at all.
 */
public class DigestClient {

    /** Shelfie answered, but with GraphQL errors and no usable data. */
    public static class ShelfieError extends RuntimeException {
        public ShelfieError(String message) { super(message); }
    }

    static final String DOCUMENT = """
        query UserReviews($id: ID!) {
          user(id: $id) { name reviews { rating book { title } } }
        }""";

    private final URI graphql;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final JsonMapper json = JsonMapper.builder().build();

    public DigestClient(URI graphql) { this.graphql = graphql; }

    /**
     * "<name>'s week" then one line per review, best first, ties by title: "5/5 Piranesi". An unknown user is
     * an empty list. GraphQL errors without data are a ShelfieError carrying the first error's message.
     */
    public List<String> digest(String userId) throws IOException, InterruptedException {
        String body = json.writeValueAsString(Map.of("query", DOCUMENT, "operationName", "UserReviews", "variables", Map.of("id", userId)));
        var request = HttpRequest.newBuilder(graphql).timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json").header("Accept", "application/graphql-response+json, application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = json.readTree(res.body());
        JsonNode user = root.path("data").path("user");
        if (root.has("errors") && (user.isMissingNode() || user.isNull()))
            throw new ShelfieError(root.get("errors").get(0).path("message").asString());
        if (user.isMissingNode() || user.isNull()) return List.of();
        record Line(int rating, String title) {}
        var lines = new ArrayList<Line>();
        for (JsonNode r : user.get("reviews")) lines.add(new Line(r.get("rating").asInt(), r.get("book").get("title").asString()));
        lines.sort(Comparator.comparingInt(Line::rating).reversed().thenComparing(Line::title));
        var out = new ArrayList<String>();
        out.add(user.get("name").asString() + "'s week");
        lines.forEach(l -> out.add(l.rating() + "/5 " + l.title()));
        return out;
    }
}
