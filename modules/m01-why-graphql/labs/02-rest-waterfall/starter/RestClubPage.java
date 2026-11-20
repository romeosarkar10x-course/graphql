import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 1.2: build the club page (../../contract/club-page.md) from the REST API.
 * Run: bin/course java RestClubPage.java c1
 *
 * REST endpoints (try them in a browser first, e.g. http://localhost:8080/api/clubs/c1):
 *   GET /api/clubs/{id}            club: ownerId, memberIds, currentBookId, ...
 *   GET /api/clubs/{id}/meetups    all meetups, soonest first, each with "upcoming": true|false
 *   GET /api/users/{id}            a user
 *   GET /api/books/{id}            a book
 *   GET /api/books/{id}/reviews    reviews, newest first, each with an authorId
 */
public class RestClubPage {

    static final String BASE = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080");
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final AtomicInteger requests = new AtomicInteger();
    static final AtomicLong bytes = new AtomicLong();

    /** GET a path and parse the JSON. Counts requests and bytes for you. */
    static JsonNode get(String path) throws Exception {
        requests.incrementAndGet();
        var req = HttpRequest.newBuilder(URI.create(BASE + path)).header("Accept", "application/json").build();
        var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        bytes.addAndGet(res.body().getBytes(StandardCharsets.UTF_8).length);
        if (res.statusCode() != 200) throw new IllegalStateException(path + " -> " + res.statusCode());
        return JSON.readTree(res.body());
    }

    public static void main(String[] args) throws Exception {
        String clubId = args.length > 0 ? args[0] : "c1";
        long start = System.nanoTime();

        JsonNode club = get("/api/clubs/" + clubId);
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("name", club.get("name").asString());
        // TODO: description, owner, memberCount, members, currentBook, latestReviews, nextMeetup

        System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(page));
        System.err.printf("requests=%d bytes=%d ms=%d%n", requests.get(), bytes.get(), (System.nanoTime() - start) / 1_000_000);
    }
}
