package dev.shelfie.perf;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.util.List;
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
        /** Set by Lookahead: the fields below were loaded with the club, in the same statement. */
        @Transient public boolean prefetched;
        @Transient public Reader owner;
        @Transient public Book currentBook;
        @Transient public List<Book> ownerPicks;
        @Transient public Integer memberCount;
        @Transient public List<Reader> members;
    }

    @Entity(name = "Reader") @jakarta.persistence.Table(name = "reader")
    public static class Reader {
        @Id public Long id;
        public String handle;
        public String name;
        @Transient public List<Review> reviews;   // prefetched by Lookahead when selected
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
        @Transient public Book book;              // prefetched by Lookahead when selected
    }
}
