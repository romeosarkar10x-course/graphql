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
        if (first == 0) return List.of();
        Specification<Book> spec = BookSpecs.of(filter);
        if (order.field() == BookOrder.Field.RELEVANCE) {
            // The rank is a function of the request's search text, so it can't be a Sort property.
            // Put it in the criteria query instead; Spring Data drops it again for count queries.
            String search = filter.search();
            spec = spec.and((r, q, cb) -> {
                q.orderBy(cb.desc(cb.function("book_rank", Float.class, r.get("title"), r.get("blurb"), BookSpecs.param(cb, search))), cb.asc(r.get("id")));
                return cb.conjunction();
            });
            return books.findBy(spec, q -> q.limit(first).all());
        }
        return books.findBy(spec, q -> q.sortBy(sort(order)).limit(first).all());
    }

    public long count(BookFilter filter) {
        return books.count(BookSpecs.of(filter));
    }

    /** Always end with id: titles, years and ratings repeat, and an order with ties is not an order. */
    static Sort sort(BookOrder order) {
        String property = switch (order.field()) {
            case TITLE -> "title";
            case PUBLISHED -> "published";
            case RATING -> "rating";
            case RELEVANCE -> throw new IllegalStateException();
        };
        var direction = order.direction() == BookOrder.Direction.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    static void validate(BookFilter f, BookOrder order, int first) {
        if (first < 0 || first > MAX_FIRST) throw new BadFilterException("first must be between 0 and " + MAX_FIRST);
        if (f == null) f = BookFilter.NONE;
        if (f.publishedFrom() != null && f.publishedTo() != null && f.publishedFrom() > f.publishedTo())
            throw new BadFilterException("publishedFrom is after publishedTo");
        if (f.minRating() != null && (f.minRating() < 1 || f.minRating() > 5))
            throw new BadFilterException("minRating must be between 1.0 and 5.0");
        if (f.search() != null && f.search().isBlank()) throw new BadFilterException("search must not be blank");
        if (order.field() == BookOrder.Field.RELEVANCE && f.search() == null)
            throw new BadFilterException("RELEVANCE needs filter.search");
    }

    public static final class BadFilterException extends RuntimeException {
        BadFilterException(String message) { super(message); }
    }
}
