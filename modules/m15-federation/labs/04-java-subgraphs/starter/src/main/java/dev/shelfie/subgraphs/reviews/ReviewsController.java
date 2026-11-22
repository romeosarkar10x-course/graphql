package dev.shelfie.subgraphs.reviews;

import dev.shelfie.subgraphs.Store;
import dev.shelfie.subgraphs.Store.Review;
import dev.shelfie.subgraphs.Store.User;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
@Profile("reviews")
class ReviewsController {
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

    // TODO (lab 15.4): Review, Book and User are entities here; the router asks for them through _entities.

    @SchemaMapping(typeName = "Book")
    List<Review> reviews(Book book) {
        return Store.reviewsForBook(book.id());
    }

    @SchemaMapping(typeName = "Book")
    int reviewCount(Book book) {
        return Store.reviewsForBook(book.id()).size();
    }

    @SchemaMapping(typeName = "Book")
    Double averageRating(Book book) {
        List<Review> rs = Store.reviewsForBook(book.id());
        return rs.isEmpty() ? null : Math.round(rs.stream().mapToInt(Review::rating).average().orElse(0) * 100) / 100.0;
    }

    @SchemaMapping(typeName = "User", field = "reviews")
    List<Review> userReviews(User user) {
        return Store.reviewsByAuthors(List.of(user.id())).get(user.id());
    }
}
