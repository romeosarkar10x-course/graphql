package dev.shelfie.filters;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import java.math.BigDecimal;
import java.util.ArrayList;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

/**
 * One small Specification per filter field, combined with and(). A null field adds nothing. There is
 * no generic "where" input: the schema names exactly what can be filtered, so every filter here has
 * an index or a cheap scan behind it, and nothing else can be asked for.
 */
final class BookSpecs {
    private BookSpecs() {}

    static Specification<Book> of(BookFilter f) {
        var parts = new ArrayList<Specification<Book>>();
        if (f == null) f = BookFilter.NONE;
        // null: any genre. []: no genre, so nothing matches (and "genre in ()" would be invalid SQL).
        if (f.genres() != null) {
            var genres = f.genres();
            parts.add(genres.isEmpty() ? (r, q, cb) -> cb.disjunction() : (r, q, cb) -> r.get("genre").in(genres));
        }
        if (f.publishedFrom() != null) { int from = f.publishedFrom(); parts.add((r, q, cb) -> cb.ge(r.get("published"), from)); }
        if (f.publishedTo() != null) { int to = f.publishedTo(); parts.add((r, q, cb) -> cb.le(r.get("published"), to)); }
        if (f.minRating() != null) { var min = BigDecimal.valueOf(f.minRating()); parts.add((r, q, cb) -> cb.ge(r.get("rating"), min)); }
        if (f.author() != null) { String author = f.author(); parts.add((r, q, cb) -> cb.equal(r.get("author"), author)); }
        if (f.search() != null) {
            String search = f.search();
            parts.add((r, q, cb) -> cb.isTrue(cb.function("book_matches", Boolean.class, r.get("title"), r.get("blurb"), param(cb, search))));
        }
        return Specification.allOf(parts);   // no parts: unrestricted
    }

    /**
     * The search text as a bind parameter. cb.literal(search) would put it into the SQL text as a quoted
     * string: Hibernate escapes the quotes, but every search becomes a different statement. Hibernate's
     * value() binds it like the values of cb.equal(path, value).
     */
    static Expression<String> param(CriteriaBuilder cb, String value) {
        return ((HibernateCriteriaBuilder) cb).value(value);
    }
}
