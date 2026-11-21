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

/**
 * Game 8a: an infinite-scroll client for a feed that keeps growing at the top while you read it.
 *   bin/course java Scroller.java [count] [url]      (start FeedServer.java first)
 */
public class Scroller {
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * The ids a user sees, top to bottom, after scrolling until `count` books are on screen.
     * `pause` runs between requests: it is the user reading, and the bot keeps posting meanwhile.
     */
    public static List<String> scroll(String url, int count, Runnable pause) throws Exception {
        var shown = new ArrayList<String>();
        String after = null;
        while (shown.size() < count) {
            if (!shown.isEmpty()) pause.run();
            // Keyset: "the 20 after the last one I showed" is the same rows however many arrived on top.
            var vars = new HashMap<String, Object>();
            vars.put("first", Math.min(20, count - shown.size()));
            vars.put("after", after);
            JsonNode feed = gql(url, "query($first: Int!, $after: String) { feed(first: $first, after: $after) { edges { node { id } } pageInfo { hasNextPage endCursor } } }", vars).path("feed");
            for (var edge : feed.path("edges")) shown.add(edge.at("/node/id").asString());
            after = feed.at("/pageInfo/endCursor").asString(null);
            if (!feed.at("/pageInfo/hasNextPage").asBoolean(false)) break;
        }
        return shown;
    }

    static JsonNode gql(String url, String query, Map<String, Object> variables) throws Exception {
        var body = JSON.writeValueAsString(Map.of("query", query, "variables", variables));
        var req = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        JsonNode res = JSON.readTree(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body());
        if (res.has("errors")) throw new IllegalStateException(res.get("errors").toString());
        return res.path("data");
    }

    public static void main(String[] args) throws Exception {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        String url = args.length > 1 ? args[1] : "http://localhost:4000/graphql";
        var ids = scroll(url, count, () -> { try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        System.out.println(ids.size() + " shown: " + ids.subList(0, Math.min(10, ids.size())) + " ...");
    }
}
