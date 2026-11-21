import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
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
        int offset = 0;
        while (shown.size() < count) {
            if (!shown.isEmpty()) pause.run();
            int limit = Math.min(20, count - shown.size());
            JsonNode page = gql(url, "query($o: Int!, $l: Int!) { feedPage(offset: $o, limit: $l) { id } }", Map.of("o", offset, "l", limit)).path("feedPage");
            for (var book : page) shown.add(book.get("id").asString());
            if (page.size() < limit) break;
            offset += limit;
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
