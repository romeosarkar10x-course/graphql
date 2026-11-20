package dev.shelfie.ref.data;

import java.util.List;

/**
 * Shelfie domain as plain records. The REST layer serialises these as-is (with every field, which is
 * the over-fetching lesson of Module 1.2); the GraphQL layer exposes only what a query selects.
 */
public final class Model {
    private Model() {}

    public record User(
            String id,
            String name,
            String handle,
            String bio,
            String avatarUrl,
            String location,
            String joinedAt,
            List<String> favouriteGenres,
            UserStats stats,
            UserPreferences preferences) {}

    public record UserStats(int booksRead, int reviewsWritten, int clubsJoined, int followers, int following) {}

    public record UserPreferences(String theme, String language, boolean emailDigest, boolean pushNotifications) {}

    public record Book(
            String id,
            String title,
            List<String> authors,
            String isbn,
            int publishedYear,
            int pageCount,
            String coverUrl,
            String publisher,
            String language,
            String synopsis,
            List<String> genres) {}

    public record Club(
            String id,
            String name,
            String description,
            String ownerId,
            List<String> memberIds,
            String currentBookId,
            String createdAt,
            String bannerUrl,
            String visibility,
            ClubRules rules) {}

    public record ClubRules(int maxMembers, boolean approvalRequired, String meetingCadence, String spoilerPolicy) {}

    public record Review(String id, String bookId, String authorId, int rating, String body, String createdAt) {}

    public record Meetup(String id, String clubId, String title, String startsAt, String location, boolean upcoming) {}
}
