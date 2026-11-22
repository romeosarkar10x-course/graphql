package dev.shelfie.bff;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** The catalogue's REST API. A 404 is an answer ("no such book"), not a failure. */
@Component
public class CatalogClient {
    public record BookDto(String id, String title, List<String> authors, String isbn, Integer publishedYear) {}

    private final RestClient rest;

    CatalogClient(@Value("${shelfie.catalog-url}") String url) {
        this.rest = RestClient.builder().baseUrl(url).build();
    }

    public BookDto book(String id) {
        return rest.get().uri("/api/books/{id}", id).exchange((request, response) ->
            response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND) ? null : response.bodyTo(BookDto.class));
    }
}
