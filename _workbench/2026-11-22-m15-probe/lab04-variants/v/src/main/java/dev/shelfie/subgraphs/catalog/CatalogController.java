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

    @EntityMapping("Book")
    Book bookEntity(@Argument String id) {
        return Store.bookById(id);
    }
}
