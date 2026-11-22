package dev.shelfie.gateway;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * The HTTP side: timeouts, a User-Agent, and a gate. After any failure the gate stays shut for the cooldown, or
 * for as long as a 429's Retry-After says: a struggling or rate-limiting API is not called again at once.
 */
@Component
public class OpenLibrary {
    public record Edition(String isbn, String title, List<String> authors, List<String> publishers, String publishDate,
                          Integer pages, List<String> subjects, String coverUrl, Instant fetchedAt, boolean stale) {
        Edition asStale() { return new Edition(isbn, title, authors, publishers, publishDate, pages, subjects, coverUrl, fetchedAt, true); }
    }
    public record Work(String key, String title, List<String> authors, Integer firstPublishYear, Integer editionCount) {}
    public record Search(int total, List<Work> hits) {}

    public static final class Unavailable extends RuntimeException {
        Unavailable(String why) { super("Open Library is unavailable: " + why); }
    }

    private final RestClient rest;
    private final Duration cooldown;
    private volatile Instant shutUntil = Instant.EPOCH;

    OpenLibrary(@Value("${gateway.openlibrary-url}") String url, @Value("${gateway.cooldown}") Duration cooldown) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofMillis(300)).build());
        factory.setReadTimeout(Duration.ofMillis(800));
        this.rest = RestClient.builder().baseUrl(url).requestFactory(factory)
            .defaultHeader("User-Agent", "Shelfie-Gateway/1.0 (course mini-project 14.10)").build();
        this.cooldown = cooldown;
    }

    /** One call for every ISBN: unknown ISBNs are simply absent from the answer. */
    public Map<String, Edition> editions(Collection<String> isbns) {
        String keys = isbns.stream().map(i -> "ISBN:" + i).collect(Collectors.joining(","));
        JsonNode body = call(() -> rest.get().uri("/api/books?bibkeys={keys}&format=json&jscmd=data", keys));
        Map<String, Edition> out = new HashMap<>();
        Instant now = Instant.now();
        for (String isbn : isbns) {
            JsonNode e = body.get("ISBN:" + isbn);
            if (e == null) continue;
            out.put(isbn, new Edition(isbn, text(e, "title"), names(e.get("authors")), names(e.get("publishers")), text(e, "publish_date"),
                e.hasNonNull("number_of_pages") ? e.get("number_of_pages").asInt() : null, names(e.get("subjects")),
                e.has("cover") ? text(e.get("cover"), "medium") : null, now, false));
        }
        return out;
    }

    public Search search(String text, int first) {
        JsonNode body = call(() -> rest.get().uri("/search.json?q={q}&limit={n}&fields=key,title,author_name,first_publish_year,edition_count,cover_i", text, first));
        List<Work> hits = new ArrayList<>();
        for (JsonNode d : body.path("docs")) {
            List<String> authors = new ArrayList<>();
            for (JsonNode a : d.path("author_name")) authors.add(a.asString());
            hits.add(new Work(text(d, "key"), text(d, "title"), authors,
                d.hasNonNull("first_publish_year") ? d.get("first_publish_year").asInt() : null,
                d.hasNonNull("edition_count") ? d.get("edition_count").asInt() : null));
        }
        return new Search(body.path("numFound").asInt(), hits.stream().limit(first).toList());
    }

    interface Request { RestClient.RequestHeadersSpec<?> spec(); }

    private JsonNode call(Request request) {
        if (Instant.now().isBefore(shutUntil)) throw new Unavailable("cooling down after a failure, not calling");
        try {
            return request.spec().exchange((req, res) -> {
                if (res.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                    long wait = parseSeconds(res.getHeaders().getFirst("Retry-After"));
                    shut(Duration.ofSeconds(Math.max(wait, cooldown.toSeconds())));
                    throw new Unavailable("rate limited (429), retry after " + wait + " s");
                }
                if (!res.getStatusCode().is2xxSuccessful()) { shut(cooldown); throw new Unavailable("HTTP " + res.getStatusCode().value()); }
                JsonNode body;
                try {
                    body = res.bodyTo(JsonNode.class);
                } catch (RuntimeException notJson) {
                    shut(cooldown);
                    throw new Unavailable("the answer is not JSON");
                }
                if (body == null || !body.isObject()) { shut(cooldown); throw new Unavailable("the answer is not a JSON object"); }
                return body;
            });
        } catch (Unavailable e) {
            throw e;
        } catch (RuntimeException e) {
            shut(cooldown);
            throw new Unavailable(e.getClass().getSimpleName());
        }
    }

    private void shut(Duration d) {
        shutUntil = Instant.now().plus(d);
    }

    static long parseSeconds(String retryAfter) {
        try {
            return retryAfter == null ? 1 : Long.parseLong(retryAfter.trim());
        } catch (NumberFormatException httpDate) {
            return 1;
        }
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
