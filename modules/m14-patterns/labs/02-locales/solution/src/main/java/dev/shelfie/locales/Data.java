package dev.shelfie.locales;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Seed data. Blurbs exist in some languages only; prices are exact decimals; instants are UTC. */
public final class Data {
    public record Price(BigDecimal amount, String currency) {}
    public record Book(String id, String title, Map<String, String> blurbs, int pages) {}
    public record Meetup(String id, String title, String bookId, Instant startsAt, int durationMinutes) {}

    public static final Map<String, Book> BOOKS = Map.of(
        "b1", new Book("b1", "The Left Hand of Darkness", Map.of(
            "en", "An envoy on a planet of perpetual winter, where no one is fixed as man or woman.",
            "de", "Ein Gesandter auf einem Planeten des ewigen Winters, auf dem niemand für immer Mann oder Frau ist.",
            "fr", "Un envoyé sur une planète d'hiver éternel, où nul n'est à jamais homme ou femme."), 304),
        "b2", new Book("b2", "Piranesi", Map.of(
            "en", "A man lives alone in a house of endless halls and tides.",
            "fr", "Un homme vit seul dans une maison aux salles infinies et aux marées."), 245),
        "b4", new Book("b4", "The Name of the Rose", Map.of(
            "en", "Murder in a fourteenth-century abbey and its labyrinthine library.",
            "de", "Morde in einer Abtei des vierzehnten Jahrhunderts und ihrer labyrinthischen Bibliothek.",
            "it", "Delitti in un'abbazia del Trecento e nella sua biblioteca labirintica."), 1234));

    /** Prices change through setPrice. */
    public static final Map<String, Price> PRICES = new ConcurrentHashMap<>(Map.of(
        "b1", new Price(new BigDecimal("11.99"), "EUR"),
        "b2", new Price(new BigDecimal("12.50"), "EUR"),
        "b4", new Price(new BigDecimal("1234.56"), "EUR")));

    public static final List<Meetup> MEETUPS = List.of(
        new Meetup("m1", "Winter on Gethen", "b1", Instant.parse("2026-12-03T18:30:00Z"), 120),
        new Meetup("m3", "New Year in the abbey", "b4", Instant.parse("2026-12-31T23:30:00Z"), 90),
        new Meetup("m2", "The night the clocks go forward", "b2", Instant.parse("2027-03-28T00:30:00Z"), 120));

    private Data() {}
}
