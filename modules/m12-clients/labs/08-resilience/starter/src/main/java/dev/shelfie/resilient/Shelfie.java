package dev.shelfie.resilient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** The plain GraphQL calls, with timeouts and nothing else. Resilient wraps them. */
public class Shelfie {
  public record Book(String id, String title) {}
  public record UserError(String code, String message) {}
  public record Review(String id) {}
  public record ReviewPayload(Review review, List<UserError> userErrors) {}
  public record BookRef(String id) {}
  public record MyReview(String id, BookRef book) {}

  private final HttpSyncGraphQlClient client;

  public Shelfie(String url, String user) {
    // Timeouts first: without them one hung connection hangs the caller forever, and no retry ever runs.
    HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofMillis(500)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofMillis(500));
    RestClient rest = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    this.client = HttpSyncGraphQlClient.builder(rest).header("X-Shelfie-User", user).build();
  }

  public Book book(String id) {
    return client.document("query book($id: ID!) { book(id: $id) { id title } }").variable("id", id)
        .retrieveSync("book").toEntity(Book.class);
  }

  public ReviewPayload createReview(String bookId, int rating, String body) {
    return client.document("mutation createReview($input: CreateReviewInput!) { createReview(input: $input) { review { id } userErrors { code message } } }")
        .variable("input", Map.of("bookId", bookId, "rating", rating, "body", body))
        .retrieveSync("createReview").toEntity(ReviewPayload.class);
  }

  public List<MyReview> reviewsOf(String userId) {
    return client.document("query mine($id: ID!) { user(id: $id) { reviews { id book { id } } } }").variable("id", userId)
        .retrieveSync("user.reviews").toEntityList(MyReview.class);
  }
}
