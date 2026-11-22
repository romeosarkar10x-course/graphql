package dev.shelfie.fed.catalog;

import dev.shelfie.fed.Data;
import dev.shelfie.fed.Data.Book;
import dev.shelfie.fed.Stats;
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
        return Data.BOOKS.values().stream().limit(Math.max(0, first)).toList();
    }

    @QueryMapping
    Book book(@Argument String id) {
        return Data.BOOKS.get(id);
    }

    /** Book has two keys: a representation carries either an id or an isbn. */
    @EntityMapping
    List<Book> book(List<Map<String, Object>> representations) {
        Stats.entity("Book", representations.size());
        return representations.stream().map(r -> r.containsKey("id")
                ? Data.BOOKS.get((String) r.get("id"))
                : Data.BOOKS.values().stream().filter(b -> b.isbn().equals(r.get("isbn"))).findFirst().orElse(null))
                .toList();
    }
}
