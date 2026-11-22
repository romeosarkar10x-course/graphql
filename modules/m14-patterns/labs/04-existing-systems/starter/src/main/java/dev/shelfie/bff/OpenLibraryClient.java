package dev.shelfie.bff;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Open Library's Books API (https://openlibrary.org/dev/docs/api/books), jscmd=data. Works on a good day.
 * TODO the bad days: a slow answer, an outage, a 200 that is not JSON. See the schema's description of openLibrary.
 */
@Component
public class OpenLibraryClient {
    public record Info(String url, Integer pages, List<String> publishers, List<String> subjects, String coverUrl) {}

    private final RestClient rest;

    OpenLibraryClient(@Value("${shelfie.openlibrary-url}") String url) {
        this.rest = RestClient.builder().baseUrl(url).build();
    }

    public Optional<Info> byIsbn(String isbn) {
        String key = "ISBN:" + isbn;
        JsonNode e = rest.get().uri("/api/books?bibkeys={key}&format=json&jscmd=data", key).retrieve().body(JsonNode.class).get(key);
        if (e == null) return Optional.empty();
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
