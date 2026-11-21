package dev.shelfie.tui;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The feed server's `feed(first, after)` connection, one page per call. */
public final class FeedClient {
    public record Book(String id, String title, String author) {}
    public record Page(List<Book> books, String endCursor, boolean hasNextPage) {}

    private static final String QUERY = """
        query Feed($first: Int!, $after: String) {
          feed(first: $first, after: $after) { edges { node { id title author } } pageInfo { hasNextPage endCursor } }
        }""";

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    private final String url;

    public FeedClient(String url) { this.url = url; }

    public Page page(int first, String after) throws IOException, InterruptedException {
        var variables = new HashMap<String, Object>();
        variables.put("first", first);
        variables.put("after", after);
        var body = json.writeValueAsString(Map.of("query", QUERY, "variables", variables));
        var request = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        JsonNode res = json.readTree(http.send(request, HttpResponse.BodyHandlers.ofString()).body());
        if (res.has("errors")) throw new IOException("feed: " + res.get("errors").path(0).path("message").asString());
        JsonNode feed = res.at("/data/feed");
        var books = new ArrayList<Book>();
        for (var edge : feed.path("edges")) {
            var n = edge.get("node");
            books.add(new Book(n.get("id").asString(), n.get("title").asString(), n.get("author").asString()));
        }
        return new Page(books, feed.at("/pageInfo/endCursor").asString(null), feed.at("/pageInfo/hasNextPage").asBoolean(false));
    }
}
