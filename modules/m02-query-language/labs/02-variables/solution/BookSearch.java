import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 2.2 reference: the document is a constant, the user's input travels as variables.
 * Run from this folder: bin/course java BookSearch.java the 3
 */
public class BookSearch {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: BookSearch <text> [limit]");
            System.exit(1);
        }
        String base = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080");
        JsonMapper json = JsonMapper.builder().build();

        // Values never touch the document. A missing limit is left out, so the server applies `= 5`.
        Map<String, Object> variables = new HashMap<>();
        variables.put("text", args[0]);
        if (args.length > 1) variables.put("first", Integer.parseInt(args[1]));

        String body = json.writeValueAsString(Map.of(
                "query", Files.readString(Path.of("book-search.graphql")),
                "operationName", "BookSearch",
                "variables", variables));
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
        JsonNode books = root.at("/data/books");
        books.forEach(b -> System.out.println(b.get("publishedYear").asInt() + "  " + b.get("title").asString()));
        System.out.println("found " + books.size());
    }
}
