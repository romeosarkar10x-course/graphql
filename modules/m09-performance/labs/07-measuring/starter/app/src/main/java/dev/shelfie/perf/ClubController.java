package dev.shelfie.perf;

import dev.shelfie.perf.Entities.Book;
import dev.shelfie.perf.Entities.Club;
import dev.shelfie.perf.Entities.Reader;
import dev.shelfie.perf.Entities.Review;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/** The naive resolvers: one repository call per field per parent. Correct, and slow. */
@Controller
class ClubController {
    private final ClubRepository clubs;
    private final ReaderRepository readers;
    private final BookRepository books;
    private final ReviewRepository reviews;

    ClubController(ClubRepository clubs, ReaderRepository readers, BookRepository books, ReviewRepository reviews) {
        this.clubs = clubs; this.readers = readers; this.books = books; this.reviews = reviews;
    }

    @QueryMapping Reader viewer(@ContextValue(required = false) Long viewer) {
        return viewer == null ? null : readers.findById(viewer).orElse(null);
    }

    @QueryMapping Club club(@Argument long id) { return clubs.findById(id).orElse(null); }

    @QueryMapping List<Club> clubs(@Argument int first) { return clubs.findAllByOrderById(Limit.of(Math.clamp(first, 1, 100))); }

    @SchemaMapping Reader owner(Club club) { return readers.findById(club.ownerId).orElseThrow(); }

    @SchemaMapping Book currentBook(Club club) {
        return club.currentBookId == null ? null : books.findById(club.currentBookId).orElseThrow();
    }

    @SchemaMapping int memberCount(Club club) { return clubs.countMembers(club.id); }

    @SchemaMapping List<Book> ownerPicks(Club club, @Argument int first) { return books.pickedBy(club.ownerId, Math.clamp(first, 1, 10)); }

    @SchemaMapping List<Reader> members(Club club, @Argument int first) { return readers.membersOf(club.id, Math.clamp(first, 1, 100)); }

    @SchemaMapping List<Review> reviews(Reader reader, @Argument int first) { return reviews.latestBy(reader.id, Math.clamp(first, 1, 100)); }

    @SchemaMapping Book book(Review review) { return books.findById(review.bookId).orElseThrow(); }

    @SchemaMapping Reader author(Review review) { return readers.findById(review.authorId).orElseThrow(); }

    @MutationMapping Book renameBook(@Argument long id, @Argument String title) {
        return books.findById(id).map(b -> { b.title = title; return books.save(b); }).orElse(null);
    }

    @MutationMapping Review addReview(@Argument long bookId, @Argument int rating, @Argument String body,
                                      @ContextValue(required = false) Long viewer) {
        if (viewer == null) throw new IllegalArgumentException("Send X-Shelfie-User: <reader id>");
        var r = new Review();
        r.bookId = bookId; r.authorId = viewer; r.rating = rating; r.body = body; r.createdAt = Instant.now();
        return reviews.save(r);
    }
}
