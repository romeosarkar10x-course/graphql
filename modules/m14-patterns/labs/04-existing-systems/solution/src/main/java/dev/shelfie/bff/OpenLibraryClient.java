package dev.shelfie.bff;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Open Library's Books API. Three defences, in order:
 *  1. timeouts (300 ms to connect, 800 ms to read): a slow answer becomes a failure, not a hung page;
 *  2. a circuit breaker: when half of the last 10 calls failed, stop calling for 3 s, then try 2 calls;
 *  3. a strict reading of the answer: anything but a JSON object is a failure, missing facts are just missing.
 * Every failure becomes Unavailable, which the controller turns into a null field and one error.
 */
@Component
public class OpenLibraryClient {
    public record Info(String url, Integer pages, List<String> publishers, List<String> subjects, String coverUrl) {}

    public static final class Unavailable extends RuntimeException {
        Unavailable(String why, Throwable cause) { super("Open Library is unavailable: " + why, cause); }
    }

    private final RestClient rest;
    private final CircuitBreaker breaker = CircuitBreaker.of("openlibrary", CircuitBreakerConfig.custom()
        .slidingWindowSize(10).minimumNumberOfCalls(4).failureRateThreshold(50)
        .waitDurationInOpenState(Duration.ofSeconds(3)).permittedNumberOfCallsInHalfOpenState(2)
        .build());

    OpenLibraryClient(@Value("${shelfie.openlibrary-url}") String url) {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(300)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(800));
        this.rest = RestClient.builder().baseUrl(url).requestFactory(factory)
            // Open Library asks API users to identify themselves (and to stay polite).
            .defaultHeader("User-Agent", "Shelfie/1.0 (course lab 14.4)")
            .build();
    }

    public Optional<Info> byIsbn(String isbn) {
        try {
            return breaker.executeSupplier(() -> fetch(isbn));
        } catch (CallNotPermittedException open) {
            throw new Unavailable("circuit open, not calling", open);
        } catch (Unavailable e) {
            throw e;
        } catch (RuntimeException e) {
            throw new Unavailable(e.getClass().getSimpleName(), e);
        }
    }

    private Optional<Info> fetch(String isbn) {
        String key = "ISBN:" + isbn;
        JsonNode body = rest.get().uri("/api/books?bibkeys={key}&format=json&jscmd=data", key).retrieve().body(JsonNode.class);
        if (body == null || !body.isObject()) throw new Unavailable("not a JSON object", null);
        JsonNode e = body.get(key);
        if (e == null) return Optional.empty();   // Open Library has no record: an answer, not a failure
        return Optional.of(new Info(text(e, "url"), e.hasNonNull("number_of_pages") ? e.get("number_of_pages").asInt() : null,
            names(e.get("publishers")), names(e.get("subjects")), e.has("cover") ? text(e.get("cover"), "medium") : null));
    }

    static String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asString() : null;
    }

    static List<String> names(JsonNode array) {
        List<String> out = new ArrayList<>();
        if (array != null) for (JsonNode n : array) if (n.hasNonNull("name")) out.add(n.get("name").asString());
        return out;
    }
}
