package dev.shelfie.data;

import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** The hand-written fetcher, for comparison: a derived query the generated fetchers cannot express. */
@Controller
public class SearchController {
    private final BookRepository books;

    SearchController(BookRepository books) { this.books = books; }

    @QueryMapping
    public List<Book> searchBooks(@Argument String titleContains) {
        return books.findByTitleContainingIgnoreCaseOrderByPublishedYear(titleContains);
    }
}
