import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Game 8a judge: snapshot the feed, let Scroller.scroll show 200 books while the bot posts (and deletes),
 * then count duplicates, gaps and order. Prints the score line: requests used.
 */
public class ScrollJudge {
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final JsonMapper JSON = JsonMapper.builder().build();

    static JsonNode post(String url, String body) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return JSON.readTree(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body());
    }
    static List<Long> all(String url) throws Exception {
        var out = new ArrayList<Long>();
        for (var id : post(url, "{\"query\":\"{ allIds }\"}").at("/data/allIds")) out.add(Long.parseLong(id.asString()));
        return out;
    }

    public static void main(String[] args) throws Exception {
        String url = args[0], base = url.replace("/graphql", ""), label = args.length > 1 ? args[1] : "";
        int count = 200;
        var before = all(url);
        post(base + "/stats", "");
        List<String> shown = Scroller.scroll(url, count, () -> { try { Thread.sleep(40); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        int requests = post(base + "/stats", "").path("requests").asInt();   // GraphQL requests since the reset
        var after = new HashSet<>(all(url));

        var ids = shown.stream().map(Long::parseLong).toList();
        int duplicates = ids.size() - new HashSet<>(ids).size();
        boolean ordered = true;
        for (int i = 1; i < ids.size(); i++) if (ids.get(i) >= ids.get(i - 1)) ordered = false;
        // A gap: in the feed when we started, still there at the end, inside the range we scrolled, never shown.
        long top = ids.isEmpty() ? 0 : Collections.max(ids), bottom = ids.isEmpty() ? 0 : Collections.min(ids);
        var seen = new HashSet<>(ids);
        int gaps = 0;
        for (long id : before) if (id <= top && id >= bottom && after.contains(id) && !seen.contains(id)) gaps++;

        boolean ok = ids.size() == count && duplicates == 0 && gaps == 0 && ordered;
        System.out.printf("%s%s: shown %d, duplicates %d, gaps %d, newest-first %s, requests %d%n",
            ok ? "ok    " : "FAIL  ", label, ids.size(), duplicates, gaps, ordered ? "yes" : "NO", requests);
        System.exit(ok ? 0 : 1);
    }
}
