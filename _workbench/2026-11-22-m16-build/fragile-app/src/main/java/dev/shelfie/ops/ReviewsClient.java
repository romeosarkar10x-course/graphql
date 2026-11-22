package dev.shelfie.ops;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** The reviews service over HTTP (contract/ReviewsService.java). One call per batch of books. */
@Component
public class ReviewsClient {
    public record Review(int seq, String id, String bookId, String authorId, int rating, String body) {}

    private final RestClient http;

    public ReviewsClient(RestClient.Builder builder, @Value("${shelfie.reviews.url:http://localhost:4100}") String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        this.http = builder.baseUrl(url).requestFactory(factory).build();
    }

    public Map<String, List<Review>> reviewsFor(List<String> bookIds) {
        return http.get().uri("/reviews?books={ids}", String.join(",", bookIds)).retrieve()
                .body(new ParameterizedTypeReference<Map<String, List<Review>>>() {});
    }

    public Review add(String bookId, String authorId, int rating, String body) {
        return http.post().uri("/reviews")
                .body(Map.of("bookId", bookId, "authorId", authorId, "rating", rating, "body", body))
                .retrieve().body(Review.class);
    }

    /** Reviews with seq > after, oldest first, waiting up to waitMs for the first one. */
    public List<Review> events(int after, long waitMs) {
        return http.get().uri("/events?after={a}&waitMs={w}", after, waitMs).retrieve()
                .body(new ParameterizedTypeReference<List<Review>>() {});
    }
}
