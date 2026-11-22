package dev.shelfie.subgraphs.reviews;

import dev.shelfie.subgraphs.Store;
import dev.shelfie.subgraphs.Store.Review;
import dev.shelfie.subgraphs.Store.User;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.dataloader.DataLoader;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.data.federation.EntityMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@Profile("reviews")
class ReviewsController {
    ReviewsController(BatchLoaderRegistry registry) {
        // Review entities: the DataLoader form. A mapped loader leaves unknown ids out of the map: they load as null.
        registry.forTypePair(String.class, Review.class)
                .registerMappedBatchLoader((ids, env) -> Mono.just(Store.reviewsByIds(ids)));
        // One loader behind Book.reviews, reviewCount and averageRating: one store query per level for all three.
        registry.<String, List<Review>>forName("reviewsByBook")
                .registerMappedBatchLoader((ids, env) -> Mono.just(Store.reviewsForBooks(ids)));
    }

    @QueryMapping
    Review review(@Argument String id) {
        return Store.reviewById(id);
    }

    @QueryMapping
    List<Review> latestReviews(@Argument int first) {
        return Store.latestReviews(first);
    }

    @SchemaMapping(typeName = "Review")
    User author(Review review) {
        return Store.author(review);
    }

    @SchemaMapping(typeName = "Review")
    Book book(Review review) {
        return new Book(review.bookId());
    }

    @EntityMapping
    CompletableFuture<Review> review(@Argument String id, DataLoader<String, Review> loader) {
        return loader.load(id);
    }

    /** The single form is fine when it costs nothing: a Book here is only its key. Unknown books are null. */
    @EntityMapping
    Book book(@Argument String id) {
        return Store.bookExists(id) ? new Book(id) : null;
    }

    @EntityMapping
    User user(@Argument String id) {
        return Store.userExists(id) ? new User(id, null) : null;
    }

    @SchemaMapping(typeName = "Book")
    CompletableFuture<List<Review>> reviews(Book book, DataLoader<String, List<Review>> reviewsByBook) {
        return reviewsByBook.load(book.id());
    }

    @SchemaMapping(typeName = "Book")
    CompletableFuture<Integer> reviewCount(Book book, DataLoader<String, List<Review>> reviewsByBook) {
        return reviewsByBook.load(book.id()).thenApply(List::size);
    }

    @SchemaMapping(typeName = "Book")
    CompletableFuture<Double> averageRating(Book book, DataLoader<String, List<Review>> reviewsByBook) {
        return reviewsByBook.load(book.id()).thenApply(rs -> rs.isEmpty() ? null
                : Math.round(rs.stream().mapToInt(Review::rating).average().orElse(0) * 100) / 100.0);
    }

    @BatchMapping(typeName = "User", field = "reviews")
    Map<User, List<Review>> userReviews(List<User> users) {
        Map<String, List<Review>> byAuthor = Store.reviewsByAuthors(users.stream().map(User::id).toList());
        return users.stream().collect(java.util.stream.Collectors.toMap(u -> u, u -> byAuthor.get(u.id()), (a, b) -> a));
    }
}
