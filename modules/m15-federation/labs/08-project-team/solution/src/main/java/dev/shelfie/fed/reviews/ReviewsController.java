package dev.shelfie.fed.reviews;

import dev.shelfie.fed.Data;
import dev.shelfie.fed.Data.Book;
import dev.shelfie.fed.Data.Review;
import dev.shelfie.fed.Data.User;
import dev.shelfie.fed.Stats;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.data.federation.EntityMapping;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

@Controller
@Profile("reviews")
class ReviewsController {
    @QueryMapping
    Review review(@Argument String id) {
        return Data.REVIEWS.get(id);
    }

    @QueryMapping
    List<Review> latestReviews(@Argument int first) {
        return Data.reviews().reversed().stream().limit(Math.max(0, first)).toList();
    }

    @MutationMapping
    Review addReview(@Argument String bookId, @Argument int rating, @Argument String body,
                     @ContextValue(name = "userId", required = false) String userId) {
        if (userId == null) throw new NotSignedIn();
        return Data.addReview(bookId, userId, rating, body);
    }

    @GraphQlExceptionHandler
    GraphQLError notSignedIn(NotSignedIn e, DataFetchingEnvironment env) {
        return GraphQLError.newError().errorType(ErrorType.UNAUTHORIZED).message("sign in to review")
                .path(env.getExecutionStepInfo().getPath()).build();
    }

    static class NotSignedIn extends RuntimeException {}

    /** Reviews knows the author's id and a copy of the handle (@provides), nothing else about them. */
    @SchemaMapping(typeName = "Review")
    User author(Review review) {
        return Data.USERS.get(review.authorId());
    }

    @SchemaMapping(typeName = "Review")
    Book book(Review review) {
        return Data.BOOKS.get(review.bookId());
    }

    @EntityMapping
    List<Review> review(@Argument List<String> idList) {
        Stats.entity("Review", idList.size());
        return idList.stream().map(Data.REVIEWS::get).toList();
    }

    @EntityMapping
    List<Book> book(@Argument List<String> idList) {
        Stats.entity("Book", idList.size());
        return idList.stream().map(Data.BOOKS::get).toList();
    }

    @EntityMapping
    List<User> user(@Argument List<String> idList) {
        Stats.entity("User", idList.size());
        return idList.stream().map(Data.USERS::get).toList();
    }

    @SchemaMapping(typeName = "Book")
    List<Review> reviews(Book book) {
        return Data.reviews().stream().filter(r -> r.bookId().equals(book.id())).toList();
    }

    @SchemaMapping(typeName = "Book")
    int reviewCount(Book book) {
        return reviews(book).size();
    }

    @SchemaMapping(typeName = "Book")
    Double averageRating(Book book) {
        List<Review> rs = reviews(book);
        return rs.isEmpty() ? null : Math.round(rs.stream().mapToInt(Review::rating).average().orElse(0) * 100) / 100.0;
    }

    /** Highest rating first, then the newest (the higher review number). */
    @SchemaMapping(typeName = "Book")
    Review topReview(Book book) {
        return reviews(book).stream()
                .max(Comparator.comparingInt(Review::rating).thenComparingInt(r -> Integer.parseInt(r.id().substring(1))))
                .orElse(null);
    }

    @SchemaMapping(typeName = "User", field = "reviews")
    List<Review> userReviews(User user) {
        return Data.reviews().stream().filter(r -> r.authorId().equals(user.id()))
                .sorted(Comparator.comparingInt(r -> Integer.parseInt(r.id().substring(1)))).toList();
    }
}
