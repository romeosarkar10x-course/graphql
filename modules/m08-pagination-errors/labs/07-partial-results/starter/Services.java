import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Lab 8.7's back ends. Don't edit: the test switches the reviews service between modes and counts calls. */
public final class Services {
    private Services() {}

    public record Club(String id, String name, String ownerId, List<String> memberIds) {}
    public record User(String id, String name) {}
    public record Review(String id, int rating, String body, String authorId) {}
    /** A batch answer: the reviews that loaded, and the ids that didn't. */
    public record Batch(List<Review> reviews, List<String> failedIds) {}

    public static final class ReviewsUnavailableException extends RuntimeException {
        public ReviewsUnavailableException() { super("reviews-db.internal:5432: connection timed out after 2000 ms"); }
    }

    public enum Mode { UP, DOWN, PARTIAL }

    /** Clubs and users: always up. load() counts calls. */
    public static final class ClubService {
        public final AtomicInteger loads = new AtomicInteger();
        static final Map<String, Club> CLUBS = Map.of(
            "c1", new Club("c1", "The Midnight Readers", "u11", List.of("u11", "u1", "u2", "u3")),
            "c2", new Club("c2", "Slow Burn Mysteries", "u7", List.of("u7", "u5")));
        static final Map<String, User> USERS = Map.of("u11", new User("u11", "Kemi Adeyemi"), "u1", new User("u1", "Asha Rao"),
            "u2", new User("u2", "Ben Okafor"), "u3", new User("u3", "Chloe Martin"), "u7", new User("u7", "Grace Kim"), "u5", new User("u5", "Elena Petrova"));

        public Club load(String id) { loads.incrementAndGet(); return CLUBS.get(id); }
        public User user(String id) { return USERS.get(id); }
    }

    /** Recent reviews of a club's books: UP, DOWN (throws), or PARTIAL (two of the five fail). */
    public static final class ReviewsService {
        public volatile Mode mode = Mode.UP;
        public final AtomicInteger calls = new AtomicInteger();
        static final List<Review> C1 = List.of(new Review("r1", 5, "Changed how I think.", "u1"), new Review("r2", 4, "Slow first half.", "u2"),
            new Review("r3", 5, "The ice crossing!", "u3"), new Review("r4", 3, "More physics, please.", "u1"), new Review("r5", 4, "Estraven deserved better.", "u11"));

        public Batch recent(String clubId) {
            calls.incrementAndGet();
            var all = clubId.equals("c1") ? C1 : List.<Review>of();
            return switch (mode) {
                case UP -> new Batch(all, List.of());
                case DOWN -> throw new ReviewsUnavailableException();
                case PARTIAL -> {
                    var ok = new ArrayList<Review>();
                    var failed = new ArrayList<String>();
                    for (var r : all) if (r.id().equals("r2") || r.id().equals("r4")) failed.add(r.id()); else ok.add(r);
                    yield new Batch(ok, failed);
                }
            };
        }
    }
}
