import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 12.5's experiment against the real Shelfie, with your NormalizedCache.java next to this file:
 *   java -cp <lib> LiveDemo.java http://shelfie-ref:8080
 * Two pages show book b1's average rating. One mutation adds a review and returns the book's new average.
 * Both pages, read from the cache, must show it: one write updated every query, no refetch.
 */
public class LiveDemo {
  static final JsonMapper JSON = JsonMapper.builder().build();
  static final HttpClient HTTP = HttpClient.newHttpClient();
  static final String BOOK_PAGE = "query BookPage { book(id: \"b1\") { __typename id title averageRating } }";
  static final String CLUB_PAGE = "query ClubPage { club(id: \"c1\") { __typename id name currentBook { __typename id title averageRating } } }";
  static final String ADD_REVIEW = "mutation AddReview($input: CreateReviewInput!) { createReview(input: $input) { review { __typename id rating book { __typename id averageRating } } userErrors { code message } } }";
  static int fails;

  @SuppressWarnings("unchecked")
  static Map<String, Object> post(String base, String query, Map<String, Object> vars) throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("query", query);
    body.put("variables", vars);
    HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/graphql"))
        .header("Content-Type", "application/json").header("X-Shelfie-User", "u11")
        .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    Map<String, Object> json = JSON.readValue(r.body(), Map.class);
    if (json.containsKey("errors")) throw new IllegalStateException("Shelfie said " + json.get("errors"));
    return (Map<String, Object>) json.get("data");
  }

  @SuppressWarnings("unchecked")
  static Object at(Map<String, Object> data, String... path) {
    Object v = data;
    for (String p : path) v = v instanceof Map<?, ?> m ? ((Map<String, Object>) m).get(p) : null;
    return v;
  }

  static void check(String name, boolean ok, String detail) {
    System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "\t" + detail));
    if (!ok) fails++;
  }

  public static void main(String[] args) throws Exception {
    String base = args[0];
    HTTP.send(HttpRequest.newBuilder(URI.create(base + "/_course/reset")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
    NormalizedCache cache = new NormalizedCache(Map.of());
    cache.write(BOOK_PAGE, Map.of(), post(base, BOOK_PAGE, Map.of()));
    cache.write(CLUB_PAGE, Map.of(), post(base, CLUB_PAGE, Map.of()));
    Object before = at(cache.read(BOOK_PAGE, Map.of()), "book", "averageRating");
    System.out.println("before: book page " + before + ", club page " + at(cache.read(CLUB_PAGE, Map.of()), "club", "currentBook", "averageRating"));

    Map<String, Object> input = Map.of("bookId", "b1", "rating", 1, "body", "Lab 12.5 says: not for me this time.");
    Map<String, Object> result = post(base, ADD_REVIEW, Map.of("input", input));
    cache.write(ADD_REVIEW, Map.of("input", input), result);
    Object server = at(result, "createReview", "review", "book", "averageRating");

    Map<String, Object> bookPage = cache.read(BOOK_PAGE, Map.of());
    Map<String, Object> clubPage = cache.read(CLUB_PAGE, Map.of());
    Object onBook = bookPage == null ? "a miss" : at(bookPage, "book", "averageRating");
    Object onClub = clubPage == null ? "a miss" : at(clubPage, "club", "currentBook", "averageRating");
    System.out.println("after:  book page " + onBook + ", club page " + onClub + " (Shelfie says " + server + ")");
    System.out.println("store:  " + JSON.writerWithDefaultPrettyPrinter().writeValueAsString(cache.extract()));
    check("the mutation result changed b1's average rating on Shelfie", server != null && !server.equals(before), "before " + before + ", after " + server);
    check("the cached book page shows the new average without a refetch", server != null && server.equals(onBook), "cache says " + onBook + ", Shelfie " + server);
    check("the cached club page shows it too: one record, two queries", server != null && server.equals(onClub), "cache says " + onClub + ", Shelfie " + server);
    Map<?, ?> stats = JSON.readValue(HTTP.send(HttpRequest.newBuilder(URI.create(base + "/_course/stats")).build(), HttpResponse.BodyHandlers.ofString()).body(), Map.class);
    check("three requests in all: two pages and one mutation", Integer.valueOf(3).equals(stats.get("graphql")), "Shelfie counted " + stats.get("graphql"));
    System.exit(fails == 0 ? 0 : 1);
  }
}
