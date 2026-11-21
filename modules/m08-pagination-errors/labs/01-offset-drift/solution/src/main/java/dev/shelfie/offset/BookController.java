package dev.shelfie.offset;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

@Controller
class BookController {
    static final int MAX_SIZE = 50;
    // A unique last key (id) makes the order total; without it, rows with equal addedAt could swap pages.
    static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("addedAt"), Sort.Order.desc("id"));
    private final BookRepository books;

    BookController(BookRepository books) { this.books = books; }

    record BookPage(List<Book> items, int page, int size, long totalCount, int totalPages, boolean hasNextPage) {}

    /** LIMIT size OFFSET page * size, plus a count(*) for totalCount: Page always runs both. */
    @QueryMapping
    BookPage books(@Argument int page, @Argument int size) {
        if (page < 0) throw new BadPageException("page must be 0 or more, got " + page);
        if (size < 1 || size > MAX_SIZE) throw new BadPageException("size must be between 1 and " + MAX_SIZE + ", got " + size);
        Page<Book> result = books.findAll(PageRequest.of(page, size, NEWEST_FIRST));
        return new BookPage(result.getContent(), page, size, result.getTotalElements(), result.getTotalPages(), result.hasNext());
    }

    @MutationMapping
    Book addBook(@Argument String title) {
        return books.save(new Book(title, Instant.now()));
    }

    @MutationMapping
    boolean deleteBook(@Argument long id) {
        if (!books.existsById(id)) return false;
        books.deleteById(id);
        return true;
    }

    @GraphQlExceptionHandler
    GraphQLError badPage(BadPageException e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.BAD_REQUEST).message(e.getMessage()).build();
    }

    static final class BadPageException extends RuntimeException {
        BadPageException(String message) { super(message); }
    }
}
