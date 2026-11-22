package dev.shelfie.app.graph;

import dev.shelfie.app.service.CatalogService;
import dev.shelfie.app.service.ReviewService;
import dev.shelfie.app.store.Records.Book;
import dev.shelfie.app.store.Records.Review;
import dev.shelfie.app.store.Records.User;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/** Book and Review fields that are not plain record components. */
@Controller
public class BookController {

    private final CatalogService catalog;
    private final ReviewService reviews;

    public BookController(CatalogService catalog, ReviewService reviews) {
        this.catalog = catalog;
        this.reviews = reviews;
    }

    @SchemaMapping public Double averageRating(Book book) { return reviews.averageRating(book.id()); }
    @SchemaMapping(typeName = "Book") public List<Review> reviews(Book book, @Argument int last) { return reviews.reviewsOf(book.id(), last); }

    @SchemaMapping public User author(Review review) { return catalog.user(review.authorId()).orElseThrow(); }
    @SchemaMapping(typeName = "Review") public Book book(Review review) { return catalog.book(review.bookId()).orElseThrow(); }
}
