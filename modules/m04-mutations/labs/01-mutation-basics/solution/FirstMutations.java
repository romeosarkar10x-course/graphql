import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 4.1: four mutations against the reference server, as Dev Patel (u4). Each selects the data the
 * screen needs back, so the whole script is exactly four requests.
 *
 *   java -cp "$(lib)" FirstMutations.java
 */
public class FirstMutations {
    static final String URL = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080") + "/graphql";
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final JsonMapper JSON = JsonMapper.builder().build();

    static JsonNode mutate(String document, Map<String, Object> variables) throws Exception {
        String body = JSON.writeValueAsString(Map.of("query", document, "variables", variables));
        HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
                .header("Content-Type", "application/json")
                .header("X-Shelfie-User", "u4")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        JsonNode res = JSON.readTree(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body());
        if (res.has("errors")) throw new IllegalStateException(res.get("errors").toString());
        JsonNode payload = res.get("data").properties().iterator().next().getValue();
        if (payload.has("userErrors") && !payload.get("userErrors").isEmpty()) throw new IllegalStateException(payload.get("userErrors").toString());
        return payload;
    }

    public static void main(String[] args) throws Exception {
        JsonNode joined = mutate("""
                mutation Join($club: ID!) {
                  joinClub(input: {clubId: $club}) { club { id name memberCount } userErrors { code message } }
                }""", Map.of("club", "c2"));
        System.out.println("joined " + joined.at("/club/name").asString() + ": " + joined.at("/club/memberCount").asInt() + " members");

        JsonNode created = mutate("""
                mutation Review($input: CreateReviewInput!) {
                  createReview(input: $input) {
                    review { id rating book { title averageRating } }
                    userErrors { field message code }
                  }
                }""", Map.of("input", Map.of("bookId", "b2", "rating", 5, "body", "Read it in two sittings. The tides!")));
        String id = created.at("/review/id").asString();
        System.out.println("review " + id + " on " + created.at("/review/book/title").asString()
                + ": average now " + created.at("/review/book/averageRating").asDouble());

        JsonNode deleted = mutate("""
                mutation Delete($id: ID!) {
                  deleteReview(input: {id: $id}) { deletedReviewId book { title averageRating } userErrors { code message } }
                }""", Map.of("id", id));
        System.out.println("deleted " + deleted.at("/deletedReviewId").asString() + ": average back to "
                + deleted.at("/book/averageRating").asDouble());

        JsonNode left = mutate("""
                mutation Leave($club: ID!) {
                  leaveClub(input: {clubId: $club}) { club { id name memberCount } userErrors { code message } }
                }""", Map.of("club", "c2"));
        System.out.println("left " + left.at("/club/name").asString() + ": " + left.at("/club/memberCount").asInt() + " members");
    }
}
