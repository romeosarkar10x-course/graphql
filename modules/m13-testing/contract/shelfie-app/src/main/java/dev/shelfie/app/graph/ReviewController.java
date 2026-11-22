package dev.shelfie.app.graph;

import dev.shelfie.app.service.ReviewService;
import dev.shelfie.app.store.Records.Review;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

/** Writes and the live feed. The rules live in ReviewService; this class maps its results to the schema. */
@Controller
public class ReviewController {

    public record CreateReviewInput(String bookId, int rating, String body) {}
    public record UserError(List<String> field, String message) {}
    public record CreateReviewPayload(Review review, List<UserError> userErrors) {}

    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) { this.reviews = reviews; }

    @MutationMapping
    public CreateReviewPayload createReview(@Argument CreateReviewInput input,
                                            @ContextValue(name = "viewer", required = false) String viewer) {
        var result = reviews.create(viewer, input.bookId(), input.rating(), input.body());
        var errors = result.problems().stream().map(p -> new UserError(p.field(), p.message())).toList();
        return new CreateReviewPayload(result.review(), errors);
    }

    @MutationMapping
    public String deleteReview(@Argument String id, @ContextValue(name = "viewer", required = false) String viewer) {
        return reviews.delete(viewer, id);
    }

    @SubscriptionMapping
    public Flux<Review> reviewAdded(@Argument String bookId) { return reviews.added(bookId); }
}
