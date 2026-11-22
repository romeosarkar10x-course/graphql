package dev.shelfie.ops;

import dev.shelfie.ops.Data.Book;
import dev.shelfie.ops.ReviewsClient.Review;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;

/** Book.reviews and Book.averageRating: one reviews-service call per batch of books. */
@Controller
public class ReviewsController {
    private final ReviewsClient reviews;

    public ReviewsController(ReviewsClient reviews) { this.reviews = reviews; }

    @BatchMapping(typeName = "Book")
    public Map<Book, List<Review>> reviews(List<Book> books) {
        return byBook(books);
    }

    @BatchMapping(typeName = "Book")
    public Map<Book, Double> averageRating(List<Book> books) {
        Map<Book, Double> out = new LinkedHashMap<>();
        byBook(books).forEach((b, list) -> out.put(b, CatalogController.average(list)));
        return out;
    }

    private Map<Book, List<Review>> byBook(List<Book> books) {
        Map<String, List<Review>> found = reviews.reviewsFor(books.stream().map(Book::id).toList());
        Map<Book, List<Review>> out = new LinkedHashMap<>();
        for (Book b : books) out.put(b, found.getOrDefault(b.id(), List.of()));
        return out;
    }
}
