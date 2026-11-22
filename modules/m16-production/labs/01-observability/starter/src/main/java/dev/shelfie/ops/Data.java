package dev.shelfie.ops;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Books, clubs and users: the reference server's seed, read-only. Reviews live in the reviews service. */
public final class Data {
    public record Book(String id, String isbn13, String title, List<String> authors, int year, int pages) {
        /** ISBN-10 from a 978- ISBN-13: drop the prefix and the check digit, add the mod-11 check digit. */
        public String isbn10() {
            if (!isbn13.startsWith("978")) return null;
            String core = isbn13.substring(3, 12);
            int sum = 0;
            for (int i = 0; i < 9; i++) sum += (10 - i) * (core.charAt(i) - '0');
            int check = (11 - sum % 11) % 11;
            return core + (check == 10 ? "X" : String.valueOf(check));
        }
    }
    public record Club(String id, String name, List<String> memberIds, String currentBookId, List<String> readingList) {}
    public record User(String id, String name, String handle) {}

    public static final Map<String, Book> BOOKS = new LinkedHashMap<>();
    public static final Map<String, Club> CLUBS = new LinkedHashMap<>();
    public static final Map<String, User> USERS = new LinkedHashMap<>();

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
        String[][] users = {{"u1", "Asha Rao", "asha"}, {"u2", "Ben Okafor", "benok"}, {"u3", "Chloe Martin", "chloem"},
            {"u4", "Dev Patel", "devp"}, {"u5", "Elena Petrova", "elenap"}, {"u6", "Farid Haddad", "farid"},
            {"u7", "Grace Kim", "gracek"}, {"u8", "Hugo Silva", "hugos"}, {"u9", "Ines Duarte", "inesd"},
            {"u10", "Jonas Berg", "jonasb"}, {"u11", "Kemi Adeyemi", "kemia"}, {"u12", "Liu Wei", "liuw"}};
        for (String[] u : users) USERS.put(u[0], new User(u[0], u[1], u[2]));
        club(new Club("c1", "The Midnight Readers", List.of("u11", "u1", "u2", "u3", "u4", "u6", "u8", "u10"), "b1",
                List.of("b1", "b7", "b6", "b2")));
        club(new Club("c2", "Slow Burn Mysteries", List.of("u7", "u5", "u9", "u12"), "b4", List.of("b4", "b3")));
        club(new Club("c3", "Booker Shortlist Survivors", List.of("u5", "u3", "u12", "u9", "u1"), "b9",
                List.of("b9", "b10", "b8", "b3", "b5")));
    }

    private static void book(String id, String isbn, String title, int year, int pages, String... authors) {
        BOOKS.put(id, new Book(id, isbn, title, List.of(authors), year, pages));
    }
    private static void club(Club c) { CLUBS.put(c.id(), c); }
    private Data() {}
}
