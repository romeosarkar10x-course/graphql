import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The new screen: one book and its latest reviews. Built against the mock server only; it reads its server
 * from SHELFIE_URL, so switching to the real Shelfie changes nothing in this file.
 *
 *   SHELFIE_URL=http://localhost:4000 bin/course java ReviewsScreen.java b1
 */
public class ReviewsScreen {

    static final String QUERY = """
        query reviewsScreen($id: ID!) {
          book(id: $id) {
            title
            authors
            publishedYear
            averageRating
            reviews(last: 3) { rating body author { handle } }
          }
        }""";

    public static void main(String[] args) throws Exception {
        String base = System.getenv().getOrDefault("SHELFIE_URL", "http://localhost:4000");
        var json = JsonMapper.builder().build();
        String body = json.writeValueAsString(Map.of("query", QUERY, "variables", Map.of("id", args[0])));
        var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/graphql"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString());
        JsonNode book = json.readTree(res.body()).path("data").path("book");
        if (book.isMissingNode() || book.isNull()) { System.out.println("No book " + args[0]); return; }
        List<String> authors = book.get("authors").valueStream().map(JsonNode::asString).toList();
        String year = book.get("publishedYear").isNull() ? "" : " (" + book.get("publishedYear").asInt() + ")";
        System.out.println(book.get("title").asString() + " by " + String.join(", ", authors) + year);
        JsonNode avg = book.get("averageRating");
        int n = book.get("reviews").size();
        System.out.println((avg.isNull() ? "No ratings" : String.format(Locale.ROOT, "Rated %.2f", avg.asDouble()))
            + ", latest " + n + (n == 1 ? " review" : " reviews"));
        for (JsonNode r : book.get("reviews"))
            System.out.println("- " + r.get("rating").asInt() + "/5 @" + r.get("author").get("handle").asString() + ": " + r.get("body").asString());
    }
}
