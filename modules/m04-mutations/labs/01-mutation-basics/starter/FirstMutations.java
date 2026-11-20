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
        // TODO: as u4, in four requests and in this order: join c2, review b2 (rating 5, any body of
        // 10+ characters), delete that review, leave c2. Select what each line below needs from the
        // mutation's own payload: no follow-up queries.
        //   joined Slow Burn Mysteries: 5 members
        //   review r16 on Piranesi: average now 4.67
        //   deleted r16: average back to 4.5
        //   left Slow Burn Mysteries: 4 members
        JsonNode joined = mutate("""
                mutation Join($club: ID!) {
                  joinClub(input: {clubId: $club}) { userErrors { code message } }
                }""", Map.of("club", "c2"));
        System.out.println("joined ?");
    }
}
