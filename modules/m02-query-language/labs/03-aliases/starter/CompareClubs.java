import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 2.3: two clubs side by side from one request. The Java side is done: it reads data.left,
 * data.right and currentBook.rating. Make compare-clubs.graphql return those keys.
 * Run from this folder: bin/course java CompareClubs.java c1 c2
 */
public class CompareClubs {

    public static void main(String[] args) throws Exception {
        String base = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080");
        JsonMapper json = JsonMapper.builder().build();
        String body = json.writeValueAsString(Map.of(
                "query", Files.readString(Path.of("compare-clubs.graphql")),
                "operationName", "CompareClubs",
                "variables", Map.of("a", args[0], "b", args[1])));
        var request = HttpRequest.newBuilder(URI.create(base + "/graphql"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/graphql-response+json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        JsonNode root = json.readTree(HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body());
        if (root.has("errors")) {
            root.get("errors").forEach(e -> System.err.println("error: " + e.get("message").asString()));
            System.exit(2);
        }
        // The aliases are the response keys: data.left and data.right.
        JsonNode left = root.at("/data/left"), right = root.at("/data/right");
        if (left.isNull() || right.isNull()) {
            System.out.println("no club " + (left.isNull() ? args[0] : args[1]));
            System.exit(3);
        }
        row("club", left.get("name").asString(), right.get("name").asString());
        row("members", left.get("memberCount").asString(), right.get("memberCount").asString());
        row("reading", title(left), title(right));
        row("rating", rating(left), rating(right));
    }

    static String title(JsonNode club) {
        JsonNode book = club.get("currentBook");
        return book.isNull() ? "-" : book.get("title").asString();
    }

    static String rating(JsonNode club) {
        JsonNode book = club.get("currentBook");
        return book.isNull() || book.get("rating").isNull() ? "-" : book.get("rating").asString();
    }

    static void row(String label, String a, String b) {
        System.out.printf("%-9s %-34s %s%n", label, a, b);
    }
}
