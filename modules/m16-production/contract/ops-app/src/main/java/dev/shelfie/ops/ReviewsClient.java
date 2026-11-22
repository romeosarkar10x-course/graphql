package dev.shelfie.ops;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * The reviews service over HTTP (contract/ReviewsService.java). One call per batch of books.
 *
 * Module 16.5: every read goes through a bulkhead (at most 16 calls in flight, no queue), a circuit breaker
 * (opens when half of the last 20 calls failed or took over 300 ms; tries again after 2 s) and a 500 ms read
 * timeout. Whatever stops a call becomes ReviewsUnavailable, which the API reports as REVIEWS_UNAVAILABLE on the
 * reviews fields only: the rest of the page still renders.
 */
@Component
public class ReviewsClient {
    public record Review(int seq, String id, String bookId, String authorId, int rating, String body) {}

    /** The reviews service did not answer in time, failed, or the breaker or bulkhead refused the call. */
    public static class ReviewsUnavailable extends RuntimeException {
        ReviewsUnavailable(Throwable cause) { super("The reviews service is unavailable", cause); }
    }

    private final RestClient http;
    private final RestClient events;
    private final CircuitBreaker breaker = CircuitBreaker.of("reviews", CircuitBreakerConfig.custom()
            .slidingWindowSize(20).minimumNumberOfCalls(10).failureRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofMillis(300)).slowCallRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(2)).permittedNumberOfCallsInHalfOpenState(3).build());
    private final Bulkhead bulkhead = Bulkhead.of("reviews", BulkheadConfig.custom()
            .maxConcurrentCalls(16).maxWaitDuration(Duration.ZERO).build());

    public ReviewsClient(RestClient.Builder builder, @Value("${shelfie.reviews.url:http://localhost:4100}") String url) {
        var timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofMillis(300));
        timeouts.setReadTimeout(Duration.ofMillis(500));
        this.http = builder.clone().baseUrl(url).requestFactory(timeouts).build();
        // The subscription feed long-polls for seconds on purpose: its own client, no read timeout, no breaker.
        var longPoll = new SimpleClientHttpRequestFactory();
        longPoll.setConnectTimeout(Duration.ofMillis(1000));
        this.events = builder.clone().baseUrl(url).requestFactory(longPoll).build();
    }

    public Map<String, List<Review>> reviewsFor(List<String> bookIds) {
        return guarded(() -> http.get().uri("/reviews?books={ids}", String.join(",", bookIds)).retrieve()
                .body(new ParameterizedTypeReference<Map<String, List<Review>>>() {}));
    }

    public Review add(String bookId, String authorId, int rating, String body) {
        return guarded(() -> http.post().uri("/reviews")
                .body(Map.of("bookId", bookId, "authorId", authorId, "rating", rating, "body", body))
                .retrieve().body(Review.class));
    }

    /** Reviews with seq > after, oldest first, waiting up to waitMs for the first one. */
    public List<Review> events(int after, long waitMs) {
        return events.get().uri("/events?after={a}&waitMs={w}", after, waitMs).retrieve()
                .body(new ParameterizedTypeReference<List<Review>>() {});
    }

    private <T> T guarded(Supplier<T> call) {
        try {
            return Bulkhead.decorateSupplier(bulkhead, CircuitBreaker.decorateSupplier(breaker, call)).get();
        } catch (RuntimeException e) {
            throw new ReviewsUnavailable(e);
        }
    }
}
