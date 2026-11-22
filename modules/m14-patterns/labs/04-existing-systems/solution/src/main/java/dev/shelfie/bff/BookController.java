package dev.shelfie.bff;

import dev.shelfie.bff.CatalogClient.BookDto;
import dev.shelfie.bff.OpenLibraryClient.Info;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

/**
 * One resolver per source. Each field fails alone: an Open Library outage costs one field, not the page.
 * The records are named after their sources (BookDto, Info), so mappings name their GraphQL type.
 */
@Controller
public class BookController {
    public record Review(String id, int rating, String body, String authorId) {}
    public record User(String id, String name) {}

    private final CatalogClient catalog;
    private final ReviewsClient reviews;
    private final OpenLibraryClient openLibrary;

    BookController(CatalogClient catalog, ReviewsClient reviews, OpenLibraryClient openLibrary) {
        this.catalog = catalog;
        this.reviews = reviews;
        this.openLibrary = openLibrary;
    }

    @QueryMapping public BookDto book(@Argument String id) { return catalog.book(id); }

    @SchemaMapping(typeName = "Book") public List<Review> reviews(BookDto book, @Argument int last) {
        return reviews.reviews(book.id()).stream().limit(Math.max(0, last))
            .map(r -> new Review(r.getId(), r.getRating(), r.getBody(), r.getAuthorId())).toList();
    }

    @SchemaMapping public User author(Review review) {
        var u = reviews.user(review.authorId());
        return new User(u.getId(), u.getName());
    }

    @SchemaMapping(typeName = "Book") public Info openLibrary(BookDto book) {
        return book.isbn() == null ? null : openLibrary.byIsbn(book.isbn()).orElse(null);
    }

    @SchemaMapping(typeName = "OpenLibraryInfo") public List<String> subjects(Info info, @Argument int first) {
        return info.subjects().stream().limit(Math.max(0, first)).toList();
    }

    @GraphQlExceptionHandler
    GraphQLError unavailable(OpenLibraryClient.Unavailable e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.INTERNAL_ERROR).message(e.getMessage())
            .extensions(Map.of("code", "UPSTREAM_UNAVAILABLE", "upstream", "openlibrary")).build();
    }
}
