package dev.shelfie.app.store;

import java.util.List;

/** The stored shapes. GraphQL fields with the same names are read from these records directly. */
public final class Records {
    private Records() {}

    public record User(String id, String name, String handle) {}
    public record Book(String id, String title, List<String> authors, String isbn, int publishedYear) {}
    public record Club(String id, String name, String description, String ownerId, List<String> memberIds,
                       String currentBookId, String visibility, String createdAt) {}
    public record Review(String id, String bookId, String authorId, int rating, String body, String createdAt) {}
    public record Meetup(String id, String clubId, String title, String startsAt, String location, boolean upcoming) {}
}
