package dev.shelfie.directives;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Seed data: the reference server's books with a price, and three members with personal data. */
public final class Data {
    public record Book(String id, String title, String genre, List<String> spineAuthors, BigDecimal priceEur) {}
    public record User(String id, String handle, String email, String phone, String libraryCard) {}

    public static final List<Book> BOOKS = List.of(
        new Book("b1", "The Left Hand of Darkness", "science fiction", List.of("Ursula K. Le Guin"), new BigDecimal("11.99")),
        new Book("b2", "Piranesi", "fantasy", List.of("Susanna Clarke"), new BigDecimal("12.50")),
        new Book("b3", "The Remains of the Day", "literary fiction", List.of("Kazuo Ishiguro"), new BigDecimal("9.99")),
        new Book("b4", "The Name of the Rose", "historical mystery", List.of("Umberto Eco"), new BigDecimal("14.25")),
        new Book("b5", "Good Omens", "comic fantasy", List.of("Terry Pratchett", "Neil Gaiman"), new BigDecimal("10.00")),
        new Book("b6", "Straße der Ölmühlen", "straße", List.of("Jörg Weiß"), new BigDecimal("0.01")));

    public static final List<User> USERS = List.of(
        new User("u1", "ada", "ada.lovelace@example.com", "+44 20 7946 0958", "LC-0001-7781"),
        new User("u2", "ben", "ben.okafor@example.org", null, "LC-0002-1123"),
        new User("u3", "kim", "k@x.io", "12", "LC-0003-9090"));

    public static final Map<String, Book> BOOK = BOOKS.stream().collect(Collectors.toMap(Book::id, Function.identity()));
    public static final Map<String, User> USER = USERS.stream().collect(Collectors.toMap(User::id, Function.identity()));

    private Data() {}
}
