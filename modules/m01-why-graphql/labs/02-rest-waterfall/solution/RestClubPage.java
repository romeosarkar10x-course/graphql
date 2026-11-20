import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 1.2 reference: the club page from the REST API. Requests that do not depend on each other go
 * out together, so the page costs 3 sequential "waves" of round trips:
 *
 *   wave 1  GET /api/clubs/{id}
 *   wave 2  GET /api/users/{owner}, GET /api/users/{member} x5, GET /api/books/{b},
 *           GET /api/books/{b}/reviews, GET /api/clubs/{id}/meetups   (all need only the club)
 *   wave 3  GET /api/users/{reviewer} x3                              (needs the reviews)
 *
 * Users are cached per run, so the owner (also the first member) is fetched once.
 * Run: bin/course java RestClubPage.java c1
 */
public class RestClubPage {

    static final String BASE = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080");
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final AtomicInteger requests = new AtomicInteger();
    static final AtomicLong bytes = new AtomicLong();
    static final Map<String, CompletableFuture<JsonNode>> userCache = new java.util.concurrent.ConcurrentHashMap<>();

    static CompletableFuture<JsonNode> get(String path) {
        requests.incrementAndGet();
        var req = HttpRequest.newBuilder(URI.create(BASE + path)).header("Accept", "application/json").build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(res -> {
            bytes.addAndGet(res.body().getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
            if (res.statusCode() != 200) throw new IllegalStateException(path + " -> " + res.statusCode());
            return JSON.readTree(res.body());
        });
    }

    static CompletableFuture<JsonNode> user(String id) {
        return userCache.computeIfAbsent(id, k -> get("/api/users/" + k));
    }

    public static void main(String[] args) {
        String clubId = args.length > 0 ? args[0] : "c1";
        long start = System.nanoTime();

        // wave 1
        JsonNode club = get("/api/clubs/" + clubId).join();

        // wave 2
        var owner = user(club.get("ownerId").asString());
        List<CompletableFuture<JsonNode>> members = new ArrayList<>();
        for (JsonNode id : club.get("memberIds")) {
            if (members.size() == 5) break;
            members.add(user(id.asString()));
        }
        String bookId = club.path("currentBookId").asString(null);
        var book = bookId == null ? CompletableFuture.<JsonNode>completedFuture(null) : get("/api/books/" + bookId);
        var meetups = get("/api/clubs/" + clubId + "/meetups");

        // still wave 2: the reviews need only the book id, which the club already gave us; wave 3: their authors
        List<Map<String, Object>> latestReviews = new ArrayList<>();
        if (bookId != null) {
            JsonNode reviews = get("/api/books/" + bookId + "/reviews").join();
            List<JsonNode> newest = new ArrayList<>();
            reviews.forEach(newest::add); // API returns newest first
            newest = newest.subList(0, Math.min(3, newest.size()));
            var authors = newest.stream().map(r -> user(r.get("authorId").asString())).toList();
            for (int i = 0; i < newest.size(); i++) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("reviewer", authors.get(i).join().get("name").asString());
                r.put("rating", newest.get(i).get("rating").asInt());
                r.put("body", newest.get(i).get("body").asString());
                latestReviews.add(r);
            }
        }

        Map<String, Object> next = null;
        for (JsonNode m : meetups.join()) { // API returns soonest first
            if (m.get("upcoming").asBoolean()) {
                next = new LinkedHashMap<>();
                next.put("title", m.get("title").asString());
                next.put("startsAt", m.get("startsAt").asString());
                break;
            }
        }

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("name", club.get("name").asString());
        page.put("description", club.path("description").asString(null));
        page.put("owner", owner.join().get("name").asString());
        page.put("memberCount", club.get("memberIds").size());
        page.put("members", members.stream().map(f -> f.join().get("name").asString()).toList());
        JsonNode b = book.join();
        page.put("currentBook", b == null ? null : Map.of(
                "title", b.get("title").asString(),
                "authors", JSON.convertValue(b.get("authors"), List.class)));
        page.put("latestReviews", latestReviews);
        page.put("nextMeetup", next);

        System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(page));
        System.err.printf("requests=%d bytes=%d ms=%d%n", requests.get(), bytes.get(), (System.nanoTime() - start) / 1_000_000);
    }
}
