package dev.shelfie.filters;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** Filtering and sorting, independent of GraphQL: the property tests call this directly. */
@Service
public class BookService {
    public static final int MAX_FIRST = 100;
    private final BookRepository books;

    BookService(BookRepository books) { this.books = books; }

    public List<Book> find(BookFilter filter, BookOrder order, int first) {
        validate(filter, order, first);
        Specification<Book> spec = BookSpecs.of(filter);
        if (order.field() == BookOrder.Field.RELEVANCE) throw new UnsupportedOperationException("TODO: lab 8.4");
        return books.findBy(spec, q -> q.sortBy(sort(order)).limit(first).all());
    }

    public long count(BookFilter filter) {
        return books.count(BookSpecs.of(filter));
    }

    static Sort sort(BookOrder order) {
        String property = switch (order.field()) {
            case TITLE -> "title";
            case PUBLISHED -> "published";
            case RATING -> "rating";
            case RELEVANCE -> throw new IllegalStateException();
        };
        return Sort.by(order.direction() == BookOrder.Direction.DESC ? Sort.Direction.DESC : Sort.Direction.ASC, property);
    }

    /** TODO: the rules in the schema's descriptions, each a BadFilterException. */
    static void validate(BookFilter f, BookOrder order, int first) {
    }

    public static final class BadFilterException extends RuntimeException {
        BadFilterException(String message) { super(message); }
    }
}
