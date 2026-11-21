package dev.shelfie.offset;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
class BookController {
    static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("addedAt"), Sort.Order.desc("id"));
    private final BookRepository books;

    BookController(BookRepository books) { this.books = books; }

    record BookPage(List<Book> items, int page, int size, long totalCount, int totalPages, boolean hasNextPage) {}

    /** Works, for now: loads every book and pages in Java. TODO: page in the database with a Pageable. */
    @QueryMapping
    BookPage books(@Argument int page, @Argument int size) {
        List<Book> all = books.findAll(NEWEST_FIRST);
        var items = all.stream().skip((long) page * size).limit(size).toList();
        int totalPages = (all.size() + size - 1) / size;
        return new BookPage(items, page, size, all.size(), totalPages, page + 1 < totalPages);
    }

    @MutationMapping
    Book addBook(@Argument String title) {
        throw new UnsupportedOperationException("TODO: lab 8.1");
    }

    @MutationMapping
    boolean deleteBook(@Argument long id) {
        throw new UnsupportedOperationException("TODO: lab 8.1");
    }
}
