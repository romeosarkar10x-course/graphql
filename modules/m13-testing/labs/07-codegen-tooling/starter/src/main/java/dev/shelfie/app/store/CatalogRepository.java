package dev.shelfie.app.store;

import dev.shelfie.app.store.Records.Book;
import dev.shelfie.app.store.Records.Club;
import dev.shelfie.app.store.Records.Meetup;
import dev.shelfie.app.store.Records.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Users, books, clubs and meetups: the reference server's seed (reference/shelfie-ref/.../Store.java), read-only.
 * Do not edit: expected outputs in every lab depend on it.
 */
@Repository
public class CatalogRepository {

    static final List<User> USERS = List.of(
        new User("u1", "Asha Rao", "asha"), new User("u2", "Ben Okafor", "benok"),
        new User("u3", "Chloe Martin", "chloem"), new User("u4", "Dev Patel", "devp"),
        new User("u5", "Elena Petrova", "elenap"), new User("u6", "Farid Haddad", "farid"),
        new User("u7", "Grace Kim", "gracek"), new User("u8", "Hugo Silva", "hugos"),
        new User("u9", "Ines Duarte", "inesd"), new User("u10", "Jonas Berg", "jonasb"),
        new User("u11", "Kemi Adeyemi", "kemia"), new User("u12", "Liu Wei", "liuw"));

    static final List<Book> BOOKS = List.of(
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

    static final List<Club> CLUBS = List.of(
        new Club("c1", "The Midnight Readers", "Speculative fiction, one book a month, arguments optional.", "u11",
                 List.of("u11", "u1", "u2", "u3", "u4", "u6", "u8", "u10"), "b1", "PUBLIC", "2025-02-14T18:00:00Z"),
        new Club("c2", "Slow Burn Mysteries", "Whodunits read at a civilised pace.", "u7",
                 List.of("u7", "u5", "u9", "u12"), "b4", "PUBLIC", "2025-05-01T19:30:00Z"),
        new Club("c3", "Booker Shortlist Survivors", "We read the whole shortlist. Every year. Send help.", "u5",
                 List.of("u5", "u3", "u12", "u9", "u1"), "b9", "PRIVATE", "2025-09-10T17:00:00Z"));

    static final List<Meetup> MEETUPS = List.of(
        new Meetup("m1", "c1", "October wrap-up: The Left Hand of Darkness", "2026-10-30T19:00:00Z", "Online", false),
        new Meetup("m2", "c1", "Left Hand of Darkness, part 2", "2026-11-27T19:00:00Z", "Blossom Book House, Bengaluru", true),
        new Meetup("m3", "c1", "December pick vote", "2026-12-11T19:00:00Z", "Online", true),
        new Meetup("m4", "c2", "Chapter 3 suspects", "2026-11-25T18:30:00Z", "Online", true),
        new Meetup("m5", "c3", "Shortlist debrief", "2026-11-29T17:00:00Z", "Shakespeare and Company, Paris", true));

    private static <T> Map<String, T> index(List<T> xs, Function<T, String> id) {
        return xs.stream().collect(Collectors.toMap(id, x -> x));
    }
    private final Map<String, User> users = index(USERS, User::id);
    private final Map<String, Book> books = index(BOOKS, Book::id);
    private final Map<String, Club> clubs = index(CLUBS, Club::id);

    public Optional<User> user(String id) { return Optional.ofNullable(users.get(id)); }
    public Optional<Book> book(String id) { return Optional.ofNullable(books.get(id)); }
    public Optional<Club> club(String id) { return Optional.ofNullable(clubs.get(id)); }

    /** In catalogue order. */
    public List<Book> books() { return BOOKS; }
    public List<Club> clubs() { return CLUBS; }
    public List<Meetup> meetups() { return MEETUPS; }
}
