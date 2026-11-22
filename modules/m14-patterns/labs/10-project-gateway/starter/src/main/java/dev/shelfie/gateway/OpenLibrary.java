package dev.shelfie.gateway;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/** Open Library's Books and Search APIs, read correctly. Nothing else yet: see the schema's comment for the rest. */
@Component
public class OpenLibrary {
    public record Edition(String isbn, String title, List<String> authors, List<String> publishers, String publishDate,
                          Integer pages, List<String> subjects, String coverUrl, Instant fetchedAt, boolean stale) {}
    public record Work(String key, String title, List<String> authors, Integer firstPublishYear, Integer editionCount) {}
    public record Search(int total, List<Work> hits) {}

    private final RestClient rest;

    OpenLibrary(@Value("${gateway.openlibrary-url}") String url) {
        this.rest = RestClient.builder().baseUrl(url).build();
    }

    public Edition edition(String isbn) {
        JsonNode e = rest.get().uri("/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data", isbn).retrieve().body(JsonNode.class).get("ISBN:" + isbn);
        if (e == null) return null;
        return new Edition(isbn, text(e, "title"), names(e.get("authors")), names(e.get("publishers")), text(e, "publish_date"),
            e.hasNonNull("number_of_pages") ? e.get("number_of_pages").asInt() : null, names(e.get("subjects")),
            e.has("cover") ? text(e.get("cover"), "medium") : null, Instant.now(), false);
    }

    public Search search(String text, int first) {
        JsonNode body = rest.get().uri("/search.json?q={q}&limit={n}&fields=key,title,author_name,first_publish_year,edition_count", text, first)
            .retrieve().body(JsonNode.class);
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

    static String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asString() : null;
    }

    static List<String> names(JsonNode array) {
        List<String> out = new ArrayList<>();
        if (array != null) for (JsonNode n : array) if (n.hasNonNull("name")) out.add(n.get("name").asString());
        return out;
    }
}
