import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 1.3 reference: the same club page as lab 1.2, from one GraphQL request.
 * The operation lives in club-page.graphql, next to this file.
 * Run: bin/course java GraphQlClubPage.java c1
 */
public class GraphQlClubPage {

    public static void main(String[] args) throws Exception {
        String base = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080");
        String clubId = args.length > 0 ? args[0] : "c1";
        JsonMapper json = JsonMapper.builder().build();
        long start = System.nanoTime();

        String body = json.writeValueAsString(Map.of(
                "query", Files.readString(Path.of("club-page.graphql")),
                "operationName", "ClubPage",
                "variables", Map.of("id", clubId)));
        var req = HttpRequest.newBuilder(URI.create(base + "/graphql"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/graphql-response+json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = json.readTree(res.body());
        if (root.has("errors")) throw new IllegalStateException("GraphQL errors: " + root.get("errors"));
        JsonNode club = root.at("/data/club");

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("name", club.get("name").asString());
        page.put("description", club.path("description").asString(null));
        page.put("owner", club.at("/owner/name").asString());
        page.put("memberCount", club.get("memberCount").asInt());
        List<String> members = new ArrayList<>();
        club.get("members").forEach(m -> members.add(m.get("name").asString()));
        page.put("members", members);

        JsonNode book = club.get("currentBook");
        List<Map<String, Object>> reviews = new ArrayList<>();
        if (book.isNull()) {
            page.put("currentBook", null);
        } else {
            page.put("currentBook", Map.of("title", book.get("title").asString(),
                    "authors", json.convertValue(book.get("authors"), List.class)));
            for (JsonNode r : book.get("reviews")) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("reviewer", r.at("/author/name").asString());
                m.put("rating", r.get("rating").asInt());
                m.put("body", r.get("body").asString());
                reviews.add(m);
            }
        }
        page.put("latestReviews", reviews);

        JsonNode meetups = club.get("meetups");
        page.put("nextMeetup", meetups.isEmpty() ? null : Map.of(
                "title", meetups.get(0).get("title").asString(),
                "startsAt", meetups.get(0).get("startsAt").asString()));

        System.out.println(json.writerWithDefaultPrettyPrinter().writeValueAsString(page));
        System.err.printf("requests=1 bytes=%d ms=%d%n",
                res.body().getBytes(StandardCharsets.UTF_8).length, (System.nanoTime() - start) / 1_000_000);
    }
}
