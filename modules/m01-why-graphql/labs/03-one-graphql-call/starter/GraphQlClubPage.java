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
 * Lab 1.3: the same club page as lab 1.2, from one GraphQL request.
 * The HTTP part is done; write the query (club-page.graphql) and map the response.
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
        // TODO: map the rest of the response onto the contract (../../contract/club-page.md)

        System.out.println(json.writerWithDefaultPrettyPrinter().writeValueAsString(page));
        System.err.printf("requests=1 bytes=%d ms=%d%n",
                res.body().getBytes(StandardCharsets.UTF_8).length, (System.nanoTime() - start) / 1_000_000);
    }
}
