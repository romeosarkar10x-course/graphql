package dev.shelfie.ops;

import dev.shelfie.ops.Data.Book;
import dev.shelfie.ops.Data.Club;
import dev.shelfie.ops.Data.User;
import dev.shelfie.ops.ReviewsClient.Review;
import java.util.List;
import java.util.Objects;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
public class CatalogController {
    private final ReviewsClient reviews;

    public CatalogController(ReviewsClient reviews) { this.reviews = reviews; }

    @QueryMapping public Book book(@Argument String id) { return Data.BOOKS.get(id); }
    @QueryMapping public List<Book> books(@Argument Integer first) { return Data.BOOKS.values().stream().limit(first == null ? 10 : first).toList(); }
    @QueryMapping public Club club(@Argument String id) { return Data.CLUBS.get(id); }
    @QueryMapping public List<Club> clubs() { return List.copyOf(Data.CLUBS.values()); }

    @QueryMapping
    public List<Book> search(@Argument String text) {
        String t = text.toLowerCase();
        return Data.BOOKS.values().stream().filter(b -> b.title().toLowerCase().contains(t)).toList();
    }

    @MutationMapping
    public Review addReview(@Argument String bookId, @Argument int rating, @Argument String body) {
        if (!Data.BOOKS.containsKey(bookId)) throw new IllegalArgumentException("No book " + bookId);
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("rating must be 1 to 5, was " + rating);
        return reviews.add(bookId, "u1", rating, body);
    }

    /** Books by the same authors or from the same decade (asks the recommender for each book). */
    @SchemaMapping
    public List<Book> similar(Book book, @Argument Integer first) throws InterruptedException {
        Thread.sleep(30);
        return Data.BOOKS.values().stream().filter(b -> !b.id().equals(book.id()))
                .filter(b -> b.authors().stream().anyMatch(book.authors()::contains) || b.year() / 10 == book.year() / 10)
                .limit(first == null ? 3 : first).toList();
    }

    @SchemaMapping public List<User> members(Club club) { return club.memberIds().stream().map(Data.USERS::get).toList(); }
    @SchemaMapping public int memberCount(Club club) { return club.memberIds().size(); }
    @SchemaMapping public Book currentBook(Club club) { return Data.BOOKS.get(club.currentBookId()); }
    @SchemaMapping public List<Book> readingList(Club club) { return club.readingList().stream().map(Data.BOOKS::get).toList(); }

    @SchemaMapping public Book book(Review review) { return Data.BOOKS.get(review.bookId()); }
    @SchemaMapping public User author(Review review) { return Data.USERS.get(review.authorId()); }

    /** Mean rating from the reviews Book.reviews already fetched (same batch, see ReviewsController). */
    public static Double average(List<Review> list) {
        return list == null || list.isEmpty() ? null
                : list.stream().filter(Objects::nonNull).mapToInt(Review::rating).average().orElse(0);
    }

}
