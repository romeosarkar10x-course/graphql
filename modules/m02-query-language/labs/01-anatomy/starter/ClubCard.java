import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 2.1: one query, mapped onto Java records that mirror its selection set.
 * Run from this folder: bin/course java ClubCard.java c1
 */
public class ClubCard {

    // The response envelope. Every GraphQL response over HTTP has this shape.
    record GraphQlResponse(Data data, List<GraphQlError> errors, Map<String, Object> extensions) {}
    record GraphQlError(String message, List<Object> path) {}

    // One record per selection set: the records mirror club-card.graphql, field for field.
    // TODO: add the club's other fields, and records for the owner and the current book.
    record Data(Club club) {}
    record Club(String name) {}

    public static void main(String[] args) throws Exception {
        String base = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080");
        String id = args.length > 0 ? args[0] : "c1";
        JsonMapper json = JsonMapper.builder().build();

        String body = json.writeValueAsString(Map.of(
                "query", Files.readString(Path.of("club-card.graphql")),
                "variables", Map.of("id", id)));
        var request = HttpRequest.newBuilder(URI.create(base + "/graphql"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/graphql-response+json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        String raw = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();

        GraphQlResponse response = json.readValue(raw, GraphQlResponse.class);
        if (response.errors() != null && !response.errors().isEmpty()) {
            response.errors().forEach(e -> System.err.println("error: " + e.message()));
            System.exit(2);
        }
        Club club = response.data().club();
        if (club == null) {
            System.out.println("no club " + id);
            System.exit(3);
        }
        System.out.print(card(club));
    }

    // TODO: print the three-line card exactly as the README shows.
    static String card(Club c) {
        return c.name() + "\n";
    }
}
