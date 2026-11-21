package dev.shelfie.data;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DataApplication {
    public static void main(String[] args) { SpringApplication.run(DataApplication.class, args); }

    /** Provided: the Shelfie seed's ten books, first author only. */
    @Bean
    ApplicationRunner seed(BookRepository books) {
        return args -> books.saveAll(java.util.List.of(
            new Book("b1", "The Left Hand of Darkness", "Ursula K. Le Guin", "9780441478125", 1969),
            new Book("b2", "Piranesi", "Susanna Clarke", "9781635575637", 2020),
            new Book("b3", "The Remains of the Day", "Kazuo Ishiguro", "9780679731726", 1989),
            new Book("b4", "The Name of the Rose", "Umberto Eco", "9780156001311", 1980),
            new Book("b5", "Good Omens", "Terry Pratchett", "9780060853976", 1990),
            new Book("b6", "Project Hail Mary", "Andy Weir", "9780593135204", 2021),
            new Book("b7", "The Dispossessed", "Ursula K. Le Guin", "9780061054884", 1974),
            new Book("b8", "Station Eleven", "Emily St. John Mandel", "9780385353304", 2014),
            new Book("b9", "The Seven Moons of Maali Almeida", "Shehan Karunatilaka", "9781324064824", 2022),
            new Book("b10", "Klara and the Sun", "Kazuo Ishiguro", "9780593318171", 2021)));
    }
}
