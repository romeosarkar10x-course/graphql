package dev.shelfie.connections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

@Controller
class BookController {
    static final int DEFAULT = 20, MAX = 100;
    private final BookRepository books;

    BookController(BookRepository books) { this.books = books; }

    /**
     * Returning a Window is enough: Spring for GraphQL turns it into edges, cursors and pageInfo.
     * The raw first/last are checked here because ScrollSubrange quietly turns a negative count into
     * "no count" and ignores before/last when first or after is present.
     */
    @QueryMapping
    Window<Book> books(ScrollSubrange subrange, @Argument Integer first, @Argument String after,
                       @Argument Integer last, @Argument String before) {
        if ((first != null || after != null) && (last != null || before != null)) throw new PageSizeException("use first/after or last/before, not both");
        for (Integer n : new Integer[]{first, last}) if (n != null && (n < 0 || n > MAX)) throw new PageSizeException(n);
        int count = subrange.count().orElse(DEFAULT);
        Map<String, ?> keys = subrange.position().map(p -> ((KeysetScrollPosition) p).getKeys()).orElse(Map.of());
        boolean forward = subrange.forward();
        int published = keys.isEmpty() ? (forward ? Integer.MIN_VALUE : Integer.MAX_VALUE) : (Integer) keys.get("published");
        long id = keys.isEmpty() ? (forward ? Long.MIN_VALUE : Long.MAX_VALUE) : (Long) keys.get("id");

        List<Book> rows = forward ? books.after(published, id, Limit.of(count + 1)) : books.before(published, id, Limit.of(count + 1));
        boolean more = rows.size() > count;                      // the extra row is only a probe
        var page = new ArrayList<>(rows.subList(0, Math.min(count, rows.size())));
        if (!forward) Collections.reverse(page);                 // fetched descending, returned ascending
        var direction = forward ? KeysetScrollPosition.Direction.FORWARD : KeysetScrollPosition.Direction.BACKWARD;
        return Window.from(page, i -> ScrollPosition.of(Map.of("published", page.get(i).getPublished(), "id", page.get(i).getId()), direction), more);
    }

    static final class PageSizeException extends IllegalArgumentException {
        PageSizeException(int n) { super("first/last must be between 0 and " + MAX + ", got " + n); }
        PageSizeException(String message) { super(message); }
    }
}
