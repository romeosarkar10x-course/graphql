package dev.shelfie.fed;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Shelfie's seed (the reference server's), split the way four teams would own it. Read-only except reviews. */
public final class Data {
    public record User(String id, String name, String handle, String city, String email) {}
    public record Book(String id, String isbn, String title, List<String> authors, int year, int pages) {}
    public record Review(String id, String bookId, String authorId, int rating, String body) {}
    public record Club(String id, String name, String ownerId, List<String> memberIds, String currentBookId) {}

    public static final Map<String, User> USERS = new LinkedHashMap<>();
    public static final Map<String, Book> BOOKS = new LinkedHashMap<>();
    public static final Map<String, Review> REVIEWS = new ConcurrentHashMap<>();
    public static final Map<String, Club> CLUBS = new LinkedHashMap<>();
    static final AtomicInteger NEXT_REVIEW = new AtomicInteger(16);

    static {
        String[][] people = {
            {"u1", "Asha Rao", "asha", "Bengaluru"}, {"u2", "Ben Okafor", "benok", "Lagos"},
            {"u3", "Chloe Martin", "chloem", "Lyon"}, {"u4", "Dev Patel", "devp", "Leicester"},
            {"u5", "Elena Petrova", "elenap", "Sofia"}, {"u6", "Farid Haddad", "farid", "Beirut"},
            {"u7", "Grace Kim", "gracek", "Seoul"}, {"u8", "Hugo Silva", "hugos", "Porto"},
            {"u9", "Ines Duarte", "inesd", "Lisbon"}, {"u10", "Jonas Berg", "jonasb", "Oslo"},
            {"u11", "Kemi Adeyemi", "kemia", "Accra"}, {"u12", "Liu Wei", "liuw", "Chengdu"}};
        for (String[] p : people) USERS.put(p[0], new User(p[0], p[1], p[2], p[3], p[2] + "@example.net"));

        book("b1", "9780441478125", "The Left Hand of Darkness", 1969, 304, "Ursula K. Le Guin");
        book("b2", "9781635575637", "Piranesi", 2020, 272, "Susanna Clarke");
        book("b3", "9780679731726", "The Remains of the Day", 1989, 245, "Kazuo Ishiguro");
        book("b4", "9780156001311", "The Name of the Rose", 1980, 536, "Umberto Eco");
        book("b5", "9780060853976", "Good Omens", 1990, 432, "Terry Pratchett", "Neil Gaiman");
        book("b6", "9780593135204", "Project Hail Mary", 2021, 496, "Andy Weir");
        book("b7", "9780061054884", "The Dispossessed", 1974, 387, "Ursula K. Le Guin");
        book("b8", "9780385353304", "Station Eleven", 2014, 333, "Emily St. John Mandel");
        book("b9", "9781324064824", "The Seven Moons of Maali Almeida", 2022, 400, "Shehan Karunatilaka");
        book("b10", "9780593318171", "Klara and the Sun", 2021, 303, "Kazuo Ishiguro");

        CLUBS.put("c1", new Club("c1", "The Midnight Readers", "u11", List.of("u11", "u1", "u2", "u3", "u4", "u6", "u8", "u10"), "b1"));
        CLUBS.put("c2", new Club("c2", "Slow Burn Mysteries", "u7", List.of("u7", "u5", "u9", "u12"), "b4"));
        CLUBS.put("c3", new Club("c3", "Booker Shortlist Survivors", "u5", List.of("u5", "u3", "u12", "u9", "u1"), "b9"));
        resetReviews();
    }

    public static synchronized void resetReviews() {
        REVIEWS.clear();
        NEXT_REVIEW.set(16);
        review("r1", "b1", "u1", 5, "Changed how I think about gender in fiction.");
        review("r2", "b1", "u2", 4, "Slow first half, devastating second.");
        review("r3", "b1", "u3", 5, "The ice crossing chapter. That's it, that's the review.");
        review("r4", "b1", "u10", 3, "Beautiful but I wanted more physics.");
        review("r5", "b1", "u8", 4, "Estraven deserved better.");
        review("r6", "b2", "u1", 5, "A house with infinite halls and one perfect narrator.");
        review("r7", "b2", "u6", 4, "Listened twice in a row.");
        review("r8", "b4", "u7", 5, "Labyrinthine in every sense.");
        review("r9", "b4", "u12", 4, "Read every Latin footnote. Worth it.");
        review("r10", "b6", "u10", 5, "Finally, physics.");
        review("r11", "b6", "u4", 4, "Rocky is the best character of the decade.");
        review("r12", "b9", "u5", 5, "Funny and furious.");
        review("r13", "b9", "u3", 4, "The afterlife as bureaucracy, brilliant.");
        review("r14", "b3", "u9", 5, "Quietly crushing.");
        review("r15", "b8", "u1", 4, "Survival is insufficient.");
    }

    public static Review addReview(String bookId, String authorId, int rating, String body) {
        Review r = new Review("r" + NEXT_REVIEW.getAndIncrement(), bookId, authorId, rating, body);
        REVIEWS.put(r.id(), r);
        return r;
    }

    /** Reviews in id order (r1, r2, ... r10), as the reference server lists them. */
    public static List<Review> reviews() {
        return REVIEWS.values().stream()
                .sorted(Comparator.comparingInt(r -> Integer.parseInt(r.id().substring(1)))).toList();
    }

    private static void book(String id, String isbn, String title, int year, int pages, String... authors) {
        BOOKS.put(id, new Book(id, isbn, title, List.of(authors), year, pages));
    }
    private static void review(String id, String bookId, String authorId, int rating, String body) {
        REVIEWS.put(id, new Review(id, bookId, authorId, rating, body));
    }
    private Data() {}
}
