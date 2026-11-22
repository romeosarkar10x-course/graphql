package dev.shelfie.app.store;

import dev.shelfie.app.store.Records.Review;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Reviews: the 15 seed reviews plus whatever createReview adds. Thread-safe; reset() restores the seed. */
@Repository
public class ReviewRepository {

    static final List<Review> SEED = List.of(
        new Review("r1", "b1", "u1", 5, "Changed how I think about gender in fiction.", "2026-10-02T21:10:00Z"),
        new Review("r2", "b1", "u2", 4, "Slow first half, devastating second.", "2026-10-05T08:30:00Z"),
        new Review("r3", "b1", "u3", 5, "The ice crossing chapter. That's it, that's the review.", "2026-10-09T22:45:00Z"),
        new Review("r4", "b1", "u10", 3, "Beautiful but I wanted more physics.", "2026-10-12T19:00:00Z"),
        new Review("r5", "b1", "u8", 4, "Estraven deserved better.", "2026-10-15T23:59:00Z"),
        new Review("r6", "b2", "u1", 5, "A house with infinite halls and one perfect narrator.", "2026-06-01T10:00:00Z"),
        new Review("r7", "b2", "u6", 4, "Listened twice in a row.", "2026-06-03T12:00:00Z"),
        new Review("r8", "b4", "u7", 5, "Labyrinthine in every sense.", "2026-08-20T20:00:00Z"),
        new Review("r9", "b4", "u12", 4, "Read every Latin footnote. Worth it.", "2026-08-22T07:15:00Z"),
        new Review("r10", "b6", "u10", 5, "Finally, physics.", "2026-04-11T18:20:00Z"),
        new Review("r11", "b6", "u4", 4, "Rocky is the best character of the decade.", "2026-04-12T21:00:00Z"),
        new Review("r12", "b9", "u5", 5, "Funny and furious.", "2026-09-30T16:40:00Z"),
        new Review("r13", "b9", "u3", 4, "The afterlife as bureaucracy, brilliant.", "2026-10-01T11:11:00Z"),
        new Review("r14", "b3", "u9", 5, "Quietly crushing.", "2026-03-03T09:09:00Z"),
        new Review("r15", "b8", "u1", 4, "Survival is insufficient.", "2026-05-05T05:05:00Z"));

    private final List<Review> reviews = new ArrayList<>(SEED);
    private int nextId = SEED.size() + 1;

    public synchronized List<Review> all() { return List.copyOf(reviews); }

    public synchronized Optional<Review> find(String id) {
        return reviews.stream().filter(r -> r.id().equals(id)).findFirst();
    }

    /** Stores a review under the next free id (r16, r17, ...) and returns it. */
    public synchronized Review add(String bookId, String authorId, int rating, String body, String createdAt) {
        var r = new Review("r" + nextId++, bookId, authorId, rating, body, createdAt);
        reviews.add(r);
        return r;
    }

    public synchronized boolean remove(String id) { return reviews.removeIf(r -> r.id().equals(id)); }

    public synchronized void reset() { reviews.clear(); reviews.addAll(SEED); nextId = SEED.size() + 1; }
}
