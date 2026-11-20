package dev.shelfie.ref.data;

import dev.shelfie.ref.data.Model.Book;
import dev.shelfie.ref.data.Model.Club;
import dev.shelfie.ref.data.Model.ClubRules;
import dev.shelfie.ref.data.Model.Meetup;
import dev.shelfie.ref.data.Model.Review;
import dev.shelfie.ref.data.Model.User;
import dev.shelfie.ref.data.Model.UserPreferences;
import dev.shelfie.ref.data.Model.UserStats;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Deterministic in-memory seed. Tests and expected outputs in modules/ depend on these exact values:
 * change them and the Module 0 and 1 expected files must change too.
 */
@Component
public class Store {

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Book> books = new LinkedHashMap<>();
    private final Map<String, Club> clubs = new LinkedHashMap<>();
    private final Map<String, Review> reviews = new LinkedHashMap<>();
    private final Map<String, Meetup> meetups = new LinkedHashMap<>();

    public Store() {
        String[][] people = {
            {"u1", "Asha Rao", "asha", "Reads sci-fi on the night bus.", "Bengaluru"},
            {"u2", "Ben Okafor", "benok", "Fantasy maps collector.", "Lagos"},
            {"u3", "Chloe Martin", "chloem", "Margins full of pencil notes.", "Lyon"},
            {"u4", "Dev Patel", "devp", "One chapter a night, no exceptions.", "Leicester"},
            {"u5", "Elena Petrova", "elenap", "Translated fiction first.", "Sofia"},
            {"u6", "Farid Haddad", "farid", "Audiobooks at 1.5x.", "Beirut"},
            {"u7", "Grace Kim", "gracek", "Mysteries, solved before page 100.", "Seoul"},
            {"u8", "Hugo Silva", "hugos", "Will argue about endings.", "Porto"},
            {"u9", "Ines Duarte", "inesd", "Poetry and long walks.", "Lisbon"},
            {"u10", "Jonas Berg", "jonasb", "Hard sci-fi or nothing.", "Oslo"},
            {"u11", "Kemi Adeyemi", "kemia", "Book club founder, serial.", "Accra"},
            {"u12", "Liu Wei", "liuw", "Reads the footnotes.", "Chengdu"},
        };
        String[] genres = {"science-fiction", "fantasy", "mystery", "literary", "poetry", "history"};
        for (int i = 0; i < people.length; i++) {
            String[] p = people[i];
            users.put(p[0], new User(
                    p[0], p[1], p[2], p[3],
                    "https://cdn.shelfie.dev/avatars/" + p[2] + ".png",
                    p[4],
                    "2025-%02d-%02dT09:00:00Z".formatted(1 + i % 12, 1 + i),
                    List.of(genres[i % genres.length], genres[(i + 2) % genres.length]),
                    new UserStats(20 + i * 7, 3 + i * 2, 1 + i % 3, 40 + i * 11, 30 + i * 5),
                    new UserPreferences(i % 2 == 0 ? "dark" : "light", "en", i % 3 == 0, i % 2 == 1)));
        }

        book("b1", "The Left Hand of Darkness", List.of("Ursula K. Le Guin"), "9780441478125", 1969, 304, "science-fiction");
        book("b2", "Piranesi", List.of("Susanna Clarke"), "9781635575637", 2020, 272, "fantasy");
        book("b3", "The Remains of the Day", List.of("Kazuo Ishiguro"), "9780679731726", 1989, 245, "literary");
        book("b4", "The Name of the Rose", List.of("Umberto Eco"), "9780156001311", 1980, 536, "mystery");
        book("b5", "Good Omens", List.of("Terry Pratchett", "Neil Gaiman"), "9780060853976", 1990, 432, "fantasy");
        book("b6", "Project Hail Mary", List.of("Andy Weir"), "9780593135204", 2021, 496, "science-fiction");
        book("b7", "The Dispossessed", List.of("Ursula K. Le Guin"), "9780061054884", 1974, 387, "science-fiction");
        book("b8", "Station Eleven", List.of("Emily St. John Mandel"), "9780385353304", 2014, 333, "literary");
        book("b9", "The Seven Moons of Maali Almeida", List.of("Shehan Karunatilaka"), "9781324064824", 2022, 400, "literary");
        book("b10", "Klara and the Sun", List.of("Kazuo Ishiguro"), "9780593318171", 2021, 303, "science-fiction");

        clubs.put("c1", new Club("c1", "The Midnight Readers",
                "Speculative fiction, one book a month, arguments optional.", "u11",
                List.of("u11", "u1", "u2", "u3", "u4", "u6", "u8", "u10"), "b1",
                "2025-02-14T18:00:00Z", "https://cdn.shelfie.dev/banners/c1.jpg", "PUBLIC",
                new ClubRules(30, false, "MONTHLY", "TAG_SPOILERS")));
        clubs.put("c2", new Club("c2", "Slow Burn Mysteries",
                "Whodunits read at a civilised pace.", "u7",
                List.of("u7", "u5", "u9", "u12"), "b4",
                "2025-05-01T19:30:00Z", "https://cdn.shelfie.dev/banners/c2.jpg", "PUBLIC",
                new ClubRules(12, true, "BIWEEKLY", "NO_SPOILERS")));
        clubs.put("c3", new Club("c3", "Booker Shortlist Survivors",
                "We read the whole shortlist. Every year. Send help.", "u5",
                List.of("u5", "u3", "u12", "u9", "u1"), "b9",
                "2025-09-10T17:00:00Z", "https://cdn.shelfie.dev/banners/c3.jpg", "PRIVATE",
                new ClubRules(8, true, "MONTHLY", "TAG_SPOILERS")));

        review("r1", "b1", "u1", 5, "Changed how I think about gender in fiction.", "2026-10-02T21:10:00Z");
        review("r2", "b1", "u2", 4, "Slow first half, devastating second.", "2026-10-05T08:30:00Z");
        review("r3", "b1", "u3", 5, "The ice crossing chapter. That's it, that's the review.", "2026-10-09T22:45:00Z");
        review("r4", "b1", "u10", 3, "Beautiful but I wanted more physics.", "2026-10-12T19:00:00Z");
        review("r5", "b1", "u8", 4, "Estraven deserved better.", "2026-10-15T23:59:00Z");
        review("r6", "b2", "u1", 5, "A house with infinite halls and one perfect narrator.", "2026-06-01T10:00:00Z");
        review("r7", "b2", "u6", 4, "Listened twice in a row.", "2026-06-03T12:00:00Z");
        review("r8", "b4", "u7", 5, "Labyrinthine in every sense.", "2026-08-20T20:00:00Z");
        review("r9", "b4", "u12", 4, "Read every Latin footnote. Worth it.", "2026-08-22T07:15:00Z");
        review("r10", "b6", "u10", 5, "Finally, physics.", "2026-04-11T18:20:00Z");
        review("r11", "b6", "u4", 4, "Rocky is the best character of the decade.", "2026-04-12T21:00:00Z");
        review("r12", "b9", "u5", 5, "Funny and furious.", "2026-09-30T16:40:00Z");
        review("r13", "b9", "u3", 4, "The afterlife as bureaucracy, brilliant.", "2026-10-01T11:11:00Z");
        review("r14", "b3", "u9", 5, "Quietly crushing.", "2026-03-03T09:09:00Z");
        review("r15", "b8", "u1", 4, "Survival is insufficient.", "2026-05-05T05:05:00Z");

        meetup("m1", "c1", "October wrap-up: The Left Hand of Darkness", "2026-10-30T19:00:00Z", "Online", false);
        meetup("m2", "c1", "Left Hand of Darkness, part 2", "2026-11-27T19:00:00Z", "Blossom Book House, Bengaluru", true);
        meetup("m3", "c1", "December pick vote", "2026-12-11T19:00:00Z", "Online", true);
        meetup("m4", "c2", "Chapter 3 suspects", "2026-11-25T18:30:00Z", "Online", true);
        meetup("m5", "c3", "Shortlist debrief", "2026-11-29T17:00:00Z", "Shakespeare and Company, Paris", true);
    }

    private void book(String id, String title, List<String> authors, String isbn, int year, int pages, String genre) {
        books.put(id, new Book(id, title, authors, isbn, year, pages,
                "https://cdn.shelfie.dev/covers/" + isbn + ".jpg",
                "Shelfie Sample Press", "en",
                "Synopsis of " + title + ". (Seed text: real synopses are long, which is the point of the REST payload size lesson.)",
                List.of(genre)));
    }

    private void review(String id, String bookId, String authorId, int rating, String body, String createdAt) {
        reviews.put(id, new Review(id, bookId, authorId, rating, body, createdAt));
    }

    private void meetup(String id, String clubId, String title, String startsAt, String location, boolean upcoming) {
        meetups.put(id, new Meetup(id, clubId, title, startsAt, location, upcoming));
    }

    public Optional<User> user(String id) { return Optional.ofNullable(users.get(id)); }
    public Optional<Book> book(String id) { return Optional.ofNullable(books.get(id)); }
    public Optional<Club> club(String id) { return Optional.ofNullable(clubs.get(id)); }

    public List<User> users() { return List.copyOf(users.values()); }
    public List<Book> books() { return List.copyOf(books.values()); }
    public List<Club> clubs() { return List.copyOf(clubs.values()); }

    /** Reviews for a book, newest first. */
    public List<Review> reviewsForBook(String bookId) {
        return reviews.values().stream()
                .filter(r -> r.bookId().equals(bookId))
                .sorted(Comparator.comparing(Review::createdAt).reversed())
                .toList();
    }

    public List<Review> reviewsByUser(String userId) {
        return reviews.values().stream()
                .filter(r -> r.authorId().equals(userId))
                .sorted(Comparator.comparing(Review::createdAt).reversed())
                .toList();
    }

    public List<Club> clubsOfUser(String userId) {
        return clubs.values().stream().filter(c -> c.memberIds().contains(userId)).toList();
    }

    /** Meetups for a club, soonest first. */
    public List<Meetup> meetupsForClub(String clubId, boolean upcomingOnly) {
        List<Meetup> out = new ArrayList<>();
        for (Meetup m : meetups.values()) {
            if (m.clubId().equals(clubId) && (!upcomingOnly || m.upcoming())) out.add(m);
        }
        out.sort(Comparator.comparing(Meetup::startsAt));
        return out;
    }

    public Double averageRating(String bookId) {
        var rs = reviewsForBook(bookId);
        if (rs.isEmpty()) return null;
        double avg = rs.stream().mapToInt(Review::rating).average().orElseThrow();
        return Math.round(avg * 100) / 100.0;
    }
}
