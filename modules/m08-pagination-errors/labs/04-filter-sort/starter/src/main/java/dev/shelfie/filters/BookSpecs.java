package dev.shelfie.filters;

import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;

/**
 * One small Specification per filter field, combined with Specification.allOf. A null field adds
 * nothing. (Spring Data 4 rejects spec.and(null), so collect them in a list.)
 */
final class BookSpecs {
    private BookSpecs() {}

    static Specification<Book> of(BookFilter f) {
        var parts = new ArrayList<Specification<Book>>();
        if (f == null) f = BookFilter.NONE;
        if (f.genres() != null) {
            var genres = f.genres();
            parts.add((r, q, cb) -> r.get("genre").in(genres));
        }
        // TODO: publishedFrom, publishedTo, minRating, author.
        // TODO: search, through the book_matches(title, blurb, q) function in schema.sql. The search
        //       text must reach PostgreSQL as a bind parameter, not inside the SQL text.
        return Specification.allOf(parts);
    }
}
