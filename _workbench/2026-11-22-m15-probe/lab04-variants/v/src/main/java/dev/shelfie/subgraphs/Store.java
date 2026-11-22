package dev.shelfie.subgraphs;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provided: Shelfie's books and reviews, as if behind a database. Every method is one "query" and is counted, so
 * the grader can see how many round trips an _entities request cost (GET /_stats). Do not change the counting.
 */
public final class Store {
    public record Book(String id, String isbn, String title, List<String> authors, int year, int pages) {}
    public record Review(String id, String bookId, String authorId, int rating, String body) {}
    /** A user as reviews knows them: the id and the handle stored with each review. */
    public record User(String id, String handle) {}

    static final AtomicInteger QUERIES = new AtomicInteger();
    static final Map<String, AtomicInteger> BY_METHOD = new TreeMap<>();

    private static final Map<String, Book> BOOKS = new LinkedHashMap<>();
    private static final List<Review> REVIEWS = new ArrayList<>();
    private static final Map<String, String> HANDLES = new LinkedHashMap<>();

    static {
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
        String[] h = {"u1", "asha", "u2", "benok", "u3", "chloem", "u4", "devp", "u5", "elenap", "u6", "farid",
            "u7", "gracek", "u8", "hugos", "u9", "inesd", "u10", "jonasb", "u11", "kemia", "u12", "liuw"};
        for (int i = 0; i < h.length; i += 2) HANDLES.put(h[i], h[i + 1]);
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

    // ---- catalog's queries
    public static List<Book> firstBooks(int n) { count("firstBooks"); return BOOKS.values().stream().limit(Math.max(0, n)).toList(); }
    public static Book bookById(String id) { count("bookById"); return BOOKS.get(id); }
    public static Book bookByIsbn(String isbn) { count("bookByIsbn"); return BOOKS.values().stream().filter(b -> b.isbn().equals(isbn)).findFirst().orElse(null); }
    /** One query for many ids and ISBNs; the map holds what was found, keyed by the id or ISBN asked for. */
    public static Map<String, Book> booksByIdOrIsbn(Collection<String> idsOrIsbns) {
        count("booksByIdOrIsbn");
        Map<String, Book> out = new HashMap<>();
        for (String k : idsOrIsbns) {
            Book b = BOOKS.containsKey(k) ? BOOKS.get(k) : BOOKS.values().stream().filter(x -> x.isbn().equals(k)).findFirst().orElse(null);
            if (b != null) out.put(k, b);
        }
        return out;
    }

    // ---- reviews' queries
    public static Review reviewById(String id) { count("reviewById"); return REVIEWS.stream().filter(r -> r.id().equals(id)).findFirst().orElse(null); }
    public static Map<String, Review> reviewsByIds(Collection<String> ids) {
        count("reviewsByIds");
        Map<String, Review> out = new HashMap<>();
        for (Review r : REVIEWS) if (ids.contains(r.id())) out.put(r.id(), r);
        return out;
    }
    /** Newest first. */
    public static List<Review> latestReviews(int n) { count("latestReviews"); return REVIEWS.reversed().stream().limit(Math.max(0, n)).toList(); }
    /** Oldest first. */
    public static List<Review> reviewsForBook(String bookId) { count("reviewsForBook"); return REVIEWS.stream().filter(r -> r.bookId().equals(bookId)).toList(); }
    /** One query for many books; every id asked for is in the map (empty list without reviews). Oldest first. */
    public static Map<String, List<Review>> reviewsForBooks(Collection<String> bookIds) {
        count("reviewsForBooks");
        Map<String, List<Review>> out = new HashMap<>();
        for (String id : bookIds) out.put(id, new ArrayList<>());
        for (Review r : REVIEWS) if (out.containsKey(r.bookId())) out.get(r.bookId()).add(r);
        return out;
    }
    public static Map<String, List<Review>> reviewsByAuthors(Collection<String> userIds) {
        count("reviewsByAuthors");
        Map<String, List<Review>> out = new HashMap<>();
        for (String id : userIds) out.put(id, new ArrayList<>());
        for (Review r : REVIEWS) if (out.containsKey(r.authorId())) out.get(r.authorId()).add(r);
        return out;
    }
    public static User author(Review r) { return new User(r.authorId(), HANDLES.get(r.authorId())); }
    public static boolean bookExists(String id) { return BOOKS.containsKey(id); }
    public static boolean userExists(String id) { return HANDLES.containsKey(id); }

    static synchronized void count(String method) {
        QUERIES.incrementAndGet();
        BY_METHOD.computeIfAbsent(method, m -> new AtomicInteger()).incrementAndGet();
    }

    private static void book(String id, String isbn, String title, int year, int pages, String... authors) {
        BOOKS.put(id, new Book(id, isbn, title, List.of(authors), year, pages));
    }
    private static void review(String id, String bookId, String authorId, int rating, String body) {
        REVIEWS.add(new Review(id, bookId, authorId, rating, body));
    }
    private Store() {}
}
