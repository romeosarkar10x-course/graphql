package dev.shelfie.connections;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

@Controller
class BookController {
    private final BookRepository books;

    BookController(BookRepository books) { this.books = books; }

    /** Returning a Window is enough: Spring for GraphQL turns it into edges, cursors and pageInfo. */
    @QueryMapping
    Window<Book> books(ScrollSubrange subrange) {
        var position = subrange.position().orElse(ScrollPosition.keyset());
        return books.findBy(position, Limit.of(subrange.count().orElse(20)), Sort.by("published", "id"));
    }
}
