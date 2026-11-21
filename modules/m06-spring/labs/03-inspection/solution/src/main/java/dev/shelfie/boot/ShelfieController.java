package dev.shelfie.boot;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/** Every Shelfie fetcher as a controller method. Fields not listed here are read from the records by name. */
@Controller
public class ShelfieController {

    @QueryMapping public String hello() { return "Hello from Shelfie"; }
    @QueryMapping public Data.@Nullable Club club(@Argument String id) { return Data.club(id).orElse(null); }
    @QueryMapping public List<Data.Club> clubs() { return Data.CLUBS; }
    @QueryMapping public Data.@Nullable User user(@Argument String id) { return Data.user(id).orElse(null); }
    @QueryMapping public Data.@Nullable Book book(@Argument String id) { return Data.book(id).orElse(null); }

    @QueryMapping
    public List<Data.Book> books(@Argument @Nullable String titleContains, @Argument @Nullable Integer first) {
        return Data.BOOKS.stream()
            .filter(b -> titleContains == null || b.title().toLowerCase().contains(titleContains.toLowerCase()))
            .limit(first == null ? 20 : Math.max(0, first)).toList();
    }

    /** The viewer id is put in GraphQLContext by ViewerInterceptor, from the X-Shelfie-User header. */
    @QueryMapping
    public Data.@Nullable User viewer(@ContextValue(name = "viewer", required = false) @Nullable String viewer) {
        return viewer == null ? null : Data.user(viewer).orElse(null);
    }

    @SchemaMapping public Data.User owner(Data.Club club) { return Data.user(club.ownerId()).orElseThrow(); }

    @SchemaMapping
    public List<Data.User> members(Data.Club club, @Argument @Nullable Integer first) {
        return club.memberIds().stream().limit(first == null ? 10 : Math.max(0, first)).map(id -> Data.user(id).orElseThrow()).toList();
    }

    @SchemaMapping public int memberCount(Data.Club club) { return club.memberIds().size(); }
    @SchemaMapping public Data.@Nullable Book currentBook(Data.Club club) { return Data.book(club.currentBookId()).orElse(null); }

    @SchemaMapping
    public List<Data.Meetup> meetups(Data.Club club, @Argument @Nullable Boolean upcomingOnly) {
        return Data.meetupsOf(club.id(), upcomingOnly == null || upcomingOnly);
    }

    @SchemaMapping(typeName = "User") public List<Data.Club> clubs(Data.User user) { return Data.clubsOf(user.id()); }
    @SchemaMapping(typeName = "User") public List<Data.Review> reviews(Data.User user) { return Data.reviewsBy(user.id()); }
    @SchemaMapping public @Nullable Double averageRating(Data.Book book) { return Data.averageRating(book.id()); }

    @SchemaMapping(typeName = "Book")
    public List<Data.Review> reviews(Data.Book book, @Argument @Nullable Integer last) {
        return Data.reviewsOf(book.id()).stream().limit(last == null ? 5 : Math.max(0, last)).toList();
    }

    @SchemaMapping public Data.User author(Data.Review review) { return Data.user(review.authorId()).orElseThrow(); }
    @SchemaMapping(typeName = "Review") public Data.Book book(Data.Review review) { return Data.book(review.bookId()).orElseThrow(); }
    @SchemaMapping(typeName = "Meetup") public Data.Club club(Data.Meetup meetup) { return Data.club(meetup.clubId()).orElseThrow(); }

    public record CreateReviewInput(String bookId, int rating, String body) {}
    public record UserError(@Nullable List<String> field, String message) {}
    public record CreateReviewPayload(Data.@Nullable Review review, List<UserError> userErrors) {}

    @MutationMapping
    public CreateReviewPayload createReview(@Argument CreateReviewInput input,
                                            @ContextValue(name = "viewer", required = false) @Nullable String viewer) {
        if (viewer == null) return error(null, "Sign in to review (X-Shelfie-User header)");
        if (Data.book(input.bookId()).isEmpty()) return error(List.of("input", "bookId"), "No book " + input.bookId());
        if (input.rating() < 1 || input.rating() > 5) return error(List.of("input", "rating"), "Rating must be 1 to 5");
        return new CreateReviewPayload(Data.addReview(input.bookId(), viewer, input.rating(), input.body()), List.of());
    }

    private static CreateReviewPayload error(@Nullable List<String> field, String message) {
        return new CreateReviewPayload(null, List.of(new UserError(field, message)));
    }
}
