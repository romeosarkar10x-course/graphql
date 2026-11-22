package dev.shelfie.app.service;

import dev.shelfie.app.store.CatalogRepository;
import dev.shelfie.app.store.Records.Review;
import dev.shelfie.app.store.ReviewRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/** Reviews: the rules for writing one, the average, and the live feed of new ones. */
@Service
public class ReviewService {

    public static final int MAX_BODY = 500;

    /** A rule a create request broke: the input path and a message for people. */
    public record Problem(List<String> field, String message) {}

    /** Either a review or the rules it broke, never both. */
    public record Result(Review review, List<Problem> problems) {
        public boolean ok() { return review != null; }
    }

    private final ReviewRepository reviews;
    private final CatalogRepository catalog;
    private final Clock clock;
    private final Sinks.Many<Review> added = Sinks.many().multicast().directBestEffort();

    public ReviewService(ReviewRepository reviews, CatalogRepository catalog, Clock clock) {
        this.reviews = reviews;
        this.catalog = catalog;
        this.clock = clock;
    }

    /** Newest first; ties broken by id, newest id first. {@code last} caps, below 0 means 0. */
    public List<Review> reviewsOf(String bookId, int last) {
        return reviews.all().stream()
            .filter(r -> r.bookId().equals(bookId))
            .sorted(Comparator.comparing(Review::createdAt).thenComparing(r -> idNumber(r.id())).reversed())
            .limit(Math.max(0, last))
            .toList();
    }

    public List<Review> reviewsBy(String userId) {
        return reviews.all().stream().filter(r -> r.authorId().equals(userId)).toList();
    }

    /** Mean rating rounded half up to 2 decimals; null when the book has no reviews. */
    public Double averageRating(String bookId) {
        var ratings = reviews.all().stream().filter(r -> r.bookId().equals(bookId)).mapToInt(Review::rating).toArray();
        if (ratings.length == 0) return null;
        double mean = (double) java.util.Arrays.stream(ratings).sum() / ratings.length;
        return Math.round(mean * 100) / 100.0;
    }

    public Review review(String id) {
        return reviews.find(id).orElseThrow(() -> new Problems.NotFound("No review " + id));
    }

    /**
     * Rules, in order: a viewer is required (nothing else is checked without one); the book must exist; the
     * rating is 1 to 5; the body, trimmed, is not blank and at most 500 characters; one review per user per
     * book. Every broken rule is reported, not just the first. A stored review is announced to subscribers.
     */
    public Result create(String viewer, String bookId, int rating, String body) {
        if (viewer == null) return new Result(null, List.of(new Problem(null, "Sign in to review (X-Shelfie-User header)")));
        var problems = new ArrayList<Problem>();
        boolean bookExists = catalog.book(bookId).isPresent();
        if (!bookExists) problems.add(new Problem(List.of("input", "bookId"), "No book " + bookId));
        if (rating < 1 || rating > 5) problems.add(new Problem(List.of("input", "rating"), "Rating must be 1 to 5"));
        String text = body == null ? "" : body.strip();
        if (text.isEmpty()) problems.add(new Problem(List.of("input", "body"), "Say something about the book"));
        else if (text.length() > MAX_BODY) problems.add(new Problem(List.of("input", "body"), "At most " + MAX_BODY + " characters"));
        if (bookExists && reviews.all().stream().anyMatch(r -> r.bookId().equals(bookId) && r.authorId().equals(viewer)))
            problems.add(new Problem(List.of("input", "bookId"), "You already reviewed this book"));
        if (!problems.isEmpty()) return new Result(null, List.copyOf(problems));
        Review r = reviews.add(bookId, viewer, rating, text, Instant.now(clock).toString());
        added.tryEmitNext(r);
        return new Result(r, List.of());
    }

    /** Only the author may delete a review. Returns the deleted review's id. */
    public String delete(String viewer, String id) {
        Review r = review(id);
        if (viewer == null || !viewer.equals(r.authorId())) throw new Problems.Forbidden("Only the author can delete review " + id);
        reviews.remove(id);
        return id;
    }

    /** New reviews as they are stored, for one book or (null) all books. */
    public Flux<Review> added(String bookId) {
        return added.asFlux().filter(r -> bookId == null || r.bookId().equals(bookId));
    }

    /** How many subscriptions are listening now. Tests wait for this before they publish. */
    public int liveSubscribers() { return added.currentSubscriberCount(); }

    private static int idNumber(String id) { return Integer.parseInt(id.substring(1)); }
}
