package dev.shelfie.subgraphs.catalog;

import dev.shelfie.subgraphs.Store;
import dev.shelfie.subgraphs.Store.Book;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.data.federation.EntityMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@Profile("catalog")
class CatalogController {
    @QueryMapping
    List<Book> books(@Argument int first) {
        return Store.firstBooks(first);
    }

    @QueryMapping
    Book book(@Argument String id) {
        return Store.bookById(id);
    }

    /**
     * The List form: one call for every Book representation in an _entities request, whichever key each one uses
     * (Book has two). One store query; the answer keeps the order of the representations, null where nothing matched.
     */
    @EntityMapping
    List<Book> book(List<Map<String, Object>> representations) {
        List<String> keys = representations.stream()
                .map(r -> (String) (r.containsKey("id") ? r.get("id") : r.get("isbn"))).toList();
        Map<String, Book> found = Store.booksByIdOrIsbn(keys);
        return keys.stream().map(found::get).toList();
    }
}
