package dev.shelfie.perf;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

/** The five tables of bin/course clubs, mapped by id only: no JPA associations, so every load is visible. */
final class Entities {
    private Entities() {}

    @Entity(name = "Club") @jakarta.persistence.Table(name = "club")
    public static class Club {
        @Id public Long id;
        public String name;
        @Column(name = "owner_id") public Long ownerId;
        @Column(name = "current_book_id") public Long currentBookId;
    }

    @Entity(name = "Reader") @jakarta.persistence.Table(name = "reader")
    public static class Reader {
        @Id public Long id;
        public String handle;
        public String name;
    }

    @Entity(name = "Book") @jakarta.persistence.Table(name = "book")
    public static class Book {
        @Id public Long id;
        public String title;
        public String author;
    }

    @Entity(name = "Review") @jakarta.persistence.Table(name = "review")
    public static class Review {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(name = "book_id") public Long bookId;
        @Column(name = "author_id") public Long authorId;
        public int rating;
        public String body;
        @Column(name = "created_at") public Instant createdAt;
    }
}
