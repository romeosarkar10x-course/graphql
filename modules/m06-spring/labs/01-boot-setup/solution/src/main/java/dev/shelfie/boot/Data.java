package dev.shelfie.boot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shelfie seed data for Module 5, as plain Java records. Same ids and values as the reference server
 * (reference/shelfie-ref/.../data/Store.java), trimmed to the fields Module 5 uses. Do not edit: the
 * expected outputs depend on it. Reviews are writable (createReview); reset() restores the seed.
 */
public final class Data {
    private Data() {}

    public record User(String id, String name, String handle) {}
    public record Book(String id, String title, List<String> authors, String isbn, int publishedYear) {}
    public record Club(String id, String name, String description, String ownerId, List<String> memberIds,
                String currentBookId, String visibility, String createdAt) {}
    public record Review(String id, String bookId, String authorId, int rating, String body, String createdAt) {}
    public record Meetup(String id, String clubId, String title, String startsAt, String location, boolean upcoming) {}

    public static final List<User> USERS = List.of(
        new User("u1", "Asha Rao", "asha"), new User("u2", "Ben Okafor", "benok"),
        new User("u3", "Chloe Martin", "chloem"), new User("u4", "Dev Patel", "devp"),
        new User("u5", "Elena Petrova", "elenap"), new User("u6", "Farid Haddad", "farid"),
        new User("u7", "Grace Kim", "gracek"), new User("u8", "Hugo Silva", "hugos"),
        new User("u9", "Ines Duarte", "inesd"), new User("u10", "Jonas Berg", "jonasb"),
        new User("u11", "Kemi Adeyemi", "kemia"), new User("u12", "Liu Wei", "liuw"));

    public static final List<Book> BOOKS = List.of(
        new Book("b1", "The Left Hand of Darkness", List.of("Ursula K. Le Guin"), "9780441478125", 1969),
        new Book("b2", "Piranesi", List.of("Susanna Clarke"), "9781635575637", 2020),
        new Book("b3", "The Remains of the Day", List.of("Kazuo Ishiguro"), "9780679731726", 1989),
        new Book("b4", "The Name of the Rose", List.of("Umberto Eco"), "9780156001311", 1980),
        new Book("b5", "Good Omens", List.of("Terry Pratchett", "Neil Gaiman"), "9780060853976", 1990),
        new Book("b6", "Project Hail Mary", List.of("Andy Weir"), "9780593135204", 2021),
        new Book("b7", "The Dispossessed", List.of("Ursula K. Le Guin"), "9780061054884", 1974),
        new Book("b8", "Station Eleven", List.of("Emily St. John Mandel"), "9780385353304", 2014),
        new Book("b9", "The Seven Moons of Maali Almeida", List.of("Shehan Karunatilaka"), "9781324064824", 2022),
        new Book("b10", "Klara and the Sun", List.of("Kazuo Ishiguro"), "9780593318171", 2021));

    public static final List<Club> CLUBS = List.of(
        new Club("c1", "The Midnight Readers", "Speculative fiction, one book a month, arguments optional.", "u11",
                 List.of("u11", "u1", "u2", "u3", "u4", "u6", "u8", "u10"), "b1", "PUBLIC", "2025-02-14T18:00:00Z"),
        new Club("c2", "Slow Burn Mysteries", "Whodunits read at a civilised pace.", "u7",
                 List.of("u7", "u5", "u9", "u12"), "b4", "PUBLIC", "2025-05-01T19:30:00Z"),
        new Club("c3", "Booker Shortlist Survivors", "We read the whole shortlist. Every year. Send help.", "u5",
                 List.of("u5", "u3", "u12", "u9", "u1"), "b9", "PRIVATE", "2025-09-10T17:00:00Z"));

    private static final List<Review> SEED_REVIEWS = List.of(
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

    public static final List<Meetup> MEETUPS = List.of(
        new Meetup("m1", "c1", "October wrap-up: The Left Hand of Darkness", "2026-10-30T19:00:00Z", "Online", false),
        new Meetup("m2", "c1", "Left Hand of Darkness, part 2", "2026-11-27T19:00:00Z", "Blossom Book House, Bengaluru", true),
        new Meetup("m3", "c1", "December pick vote", "2026-12-11T19:00:00Z", "Online", true),
        new Meetup("m4", "c2", "Chapter 3 suspects", "2026-11-25T18:30:00Z", "Online", true),
        new Meetup("m5", "c3", "Shortlist debrief", "2026-11-29T17:00:00Z", "Shakespeare and Company, Paris", true));

    /** All reviews, seed first, then new ones in creation order. Synchronized: fetchers may run on many threads. */
    public static final List<Review> REVIEWS = Collections.synchronizedList(new ArrayList<>(SEED_REVIEWS));

    public static void reset() { synchronized (REVIEWS) { REVIEWS.clear(); REVIEWS.addAll(SEED_REVIEWS); } }

    /** Adds a review with the next free id (r16, r17, ...) and a fixed timestamp, so outputs are reproducible. */
    public static Review addReview(String bookId, String authorId, int rating, String body) {
        synchronized (REVIEWS) {
            var r = new Review("r" + (REVIEWS.size() + 1), bookId, authorId, rating, body, "2026-11-21T12:00:00Z");
            REVIEWS.add(r);
            return r;
        }
    }

    private static <T> Map<String, T> index(List<T> xs, Function<T, String> id) {
        return xs.stream().collect(Collectors.toMap(id, x -> x));
    }
    private static final Map<String, User> USER_BY_ID = index(USERS, User::id);
    private static final Map<String, Book> BOOK_BY_ID = index(BOOKS, Book::id);
    private static final Map<String, Club> CLUB_BY_ID = index(CLUBS, Club::id);

    public static Optional<User> user(String id) { return Optional.ofNullable(USER_BY_ID.get(id)); }
    public static Optional<Book> book(String id) { return Optional.ofNullable(BOOK_BY_ID.get(id)); }
    public static Optional<Club> club(String id) { return Optional.ofNullable(CLUB_BY_ID.get(id)); }

    /** Newest first. */
    public static List<Review> reviewsOf(String bookId) {
        synchronized (REVIEWS) { return REVIEWS.stream().filter(r -> r.bookId().equals(bookId))
                .sorted(Comparator.comparing(Review::createdAt).reversed().thenComparing(Review::id, Comparator.reverseOrder())).toList(); }
    }

    public static List<Review> reviewsBy(String userId) {
        synchronized (REVIEWS) { return REVIEWS.stream().filter(r -> r.authorId().equals(userId)).toList(); }
    }

    public static List<Club> clubsOf(String userId) {
        return CLUBS.stream().filter(c -> c.memberIds().contains(userId)).toList();
    }

    /** In date order; only upcoming ones when upcomingOnly. */
    public static List<Meetup> meetupsOf(String clubId, boolean upcomingOnly) {
        return MEETUPS.stream().filter(m -> m.clubId().equals(clubId) && (!upcomingOnly || m.upcoming()))
                .sorted(Comparator.comparing(Meetup::startsAt)).toList();
    }

    /** Mean rating rounded to 2 decimals, null when there are no reviews. */
    public static Double averageRating(String bookId) {
        var rs = reviewsOf(bookId);
        if (rs.isEmpty()) return null;
        return Math.round(rs.stream().mapToInt(Review::rating).average().orElseThrow() * 100) / 100.0;
    }
}
