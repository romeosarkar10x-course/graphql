package dev.shelfie.fed.catalog;

import dev.shelfie.fed.Data;
import dev.shelfie.fed.Data.Book;
import dev.shelfie.fed.Data.Review;
import dev.shelfie.fed.Stats;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.data.federation.EntityMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * History: reviews once lived in the catalogue's database, and the link from a review to its book stayed with the
 * catalog team. With --shelfie.catalog.review-book=true (and a schema that declares it) catalog resolves
 * Review.book; lab 15.7 moves it to reviews with @override.
 */
@Controller
@Profile("catalog")
@ConditionalOnProperty("shelfie.catalog.review-book")
class ReviewBookController {
    @EntityMapping
    List<Review> review(@Argument List<String> idList) {
        Stats.entity("Review", idList.size());
        return idList.stream().map(Data.REVIEWS::get).toList();
    }

    @SchemaMapping(typeName = "Review")
    Book book(Review review) {
        return Data.BOOKS.get(review.bookId());
    }
}
