package dev.shelfie.bff;

import dev.shelfie.bff.CatalogClient.BookDto;
import dev.shelfie.bff.OpenLibraryClient.Info;
import dev.shelfie.bff.ReviewsClient.ReviewData;
import dev.shelfie.bff.ReviewsClient.UserData;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/** One resolver per source. The records are named after their sources, so mappings name their GraphQL type. */
@Controller
public class BookController {
    private final CatalogClient catalog;
    private final ReviewsClient reviews;
    private final OpenLibraryClient openLibrary;

    BookController(CatalogClient catalog, ReviewsClient reviews, OpenLibraryClient openLibrary) {
        this.catalog = catalog;
        this.reviews = reviews;
        this.openLibrary = openLibrary;
    }

    @QueryMapping public BookDto book(@Argument String id) { return catalog.book(id); }

    @SchemaMapping(typeName = "Book") public List<ReviewData> reviews(BookDto book, @Argument int last) {
        return reviews.reviews(book.id()).stream().limit(Math.max(0, last)).toList();
    }

    @SchemaMapping(typeName = "Review") public UserData author(ReviewData review) {
        return reviews.user(review.authorId());
    }

    @SchemaMapping(typeName = "Book") public Info openLibrary(BookDto book) {
        return book.isbn() == null ? null : openLibrary.byIsbn(book.isbn()).orElse(null);
    }

    @SchemaMapping(typeName = "OpenLibraryInfo") public List<String> subjects(Info info, @Argument int first) {
        return info.subjects().stream().limit(Math.max(0, first)).toList();
    }
}
