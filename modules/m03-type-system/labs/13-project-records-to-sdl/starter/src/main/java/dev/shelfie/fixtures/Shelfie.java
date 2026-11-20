package dev.shelfie.fixtures;

import dev.shelfie.sdl.Description;
import dev.shelfie.sdl.Id;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * The generator's input: Shelfie as Java types. Nested in one file so the sealed interfaces need no
 * permits clause. Do not edit; tests/ compares the SDL generated from these with the expected schema.
 */
public final class Shelfie {
    private Shelfie() {}

    @Description("Entry points.")
    public record Query(
            @Description("All clubs, in seed order.") @NonNull List<@NonNull Club> clubs,
            Optional<Club> featured,
            @NonNull List<@NonNull FeedItem> feed,
            List<Node> nodes) {}

    @Description("Anything with a global id.")
    public sealed interface Node {
        @Id @NonNull String id();
    }

    @Description("A reading group.")
    public record Club(
            @Id @NonNull String id,
            @NonNull String name,
            String description,
            @NonNull User owner,
            @NonNull List<@NonNull User> members,
            int memberCount,
            Book currentBook,
            @NonNull Visibility visibility) implements Node {}

    public record User(@Id @NonNull String id, @NonNull String name, @NonNull String handle, String bio) implements Node {}

    public record Book(
            @Id @NonNull String id,
            @NonNull String title,
            @NonNull List<@NonNull String> authors,
            Integer publishedYear,
            @Description("Mean rating, null when there are no reviews.") Double averageRating,
            @NonNull List<@NonNull Review> reviews) implements Node {}

    public record Review(@Id @NonNull String id, int rating, @NonNull String body, @NonNull User author, boolean spoiler) {}

    public enum Visibility {
        PUBLIC,
        @Description("Members only.") PRIVATE
    }

    @Description("One entry in a club's activity feed.")
    public sealed interface FeedItem {}

    public record ReviewPosted(@NonNull Review review, @NonNull Club club) implements FeedItem {}

    public record MemberJoined(@NonNull User member, @NonNull Club club) implements FeedItem {}

    /** Not reachable from Query: generated only when named as a root. Uses a type GraphQL has no scalar for. */
    public record Analytics(@NonNull String bookId, long pageViews) {}
}
