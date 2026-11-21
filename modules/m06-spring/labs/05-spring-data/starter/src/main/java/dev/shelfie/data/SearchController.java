package dev.shelfie.data;

import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** TODO: searchBooks by hand: case-insensitive "title contains", ordered by publishedYear, in SQL. */
@Controller
public class SearchController {
    @QueryMapping
    public List<Book> searchBooks(@Argument String titleContains) { return List.of(); }
}
