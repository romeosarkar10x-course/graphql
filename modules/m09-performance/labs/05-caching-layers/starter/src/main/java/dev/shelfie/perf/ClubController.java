package dev.shelfie.perf;

import dev.shelfie.perf.Entities.Book;
import dev.shelfie.perf.Entities.Club;
import dev.shelfie.perf.Entities.Reader;
import dev.shelfie.perf.Entities.Review;
import java.time.Instant;
import java.util.List;
import org.dataloader.DataLoader;
import org.springframework.data.domain.Limit;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** Root fields and mutations. Every field below the root is batched in BatchController. */
@Controller
class ClubController {
    private final ClubRepository clubs;
    private final ReaderRepository readers;
    private final BookRepository books;
    private final ReviewRepository reviews;
    private final BookCache bookCache;
    private final ResponseCache responses;

    ClubController(ClubRepository clubs, ReaderRepository readers, BookRepository books, ReviewRepository reviews,
                   BookCache bookCache, ResponseCache responses) {
        this.clubs = clubs; this.readers = readers; this.books = books; this.reviews = reviews;
        this.bookCache = bookCache; this.responses = responses;
    }

    @QueryMapping Reader viewer(@ContextValue(required = false) Long viewer) {
        return viewer == null ? null : readers.findById(viewer).orElse(null);
    }

    @QueryMapping Club club(@Argument long id) { return clubs.findById(id).orElse(null); }

    @QueryMapping List<Club> clubs(@Argument int first) { return clubs.findAllByOrderById(Limit.of(Math.clamp(first, 1, 100))); }

    @MutationMapping Book renameBook(@Argument long id, @Argument String title, DataLoader<Long, Book> books) {
        return bookCache.findById(id).map(b -> { b.title = title; return bookCache.save(b); }).orElse(null);
    }

    @MutationMapping Review addReview(@Argument long bookId, @Argument int rating, @Argument String body,
                                      @ContextValue(required = false) Long viewer) {
        if (viewer == null) throw new IllegalArgumentException("Send X-Shelfie-User: <reader id>");
        var r = new Review();
        r.bookId = bookId; r.authorId = viewer; r.rating = rating; r.body = body; r.createdAt = Instant.now();
        return reviews.save(r);
    }
}
