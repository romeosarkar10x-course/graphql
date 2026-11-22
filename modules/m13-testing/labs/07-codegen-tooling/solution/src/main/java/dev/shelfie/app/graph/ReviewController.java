package dev.shelfie.app.graph;

import dev.shelfie.app.generated.DgsConstants.MUTATION;
import dev.shelfie.app.generated.DgsConstants.SUBSCRIPTION;
import dev.shelfie.app.generated.types.CreateReviewInput;
import dev.shelfie.app.generated.types.UserError;
import dev.shelfie.app.service.ReviewService;
import dev.shelfie.app.store.Records.Review;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

/**
 * Writes and the live feed. The input and the error type are generated from the schema; the payload stays a
 * record because its review is the store's record, not the generated Review class.
 */
@Controller
public class ReviewController {

    public record CreateReviewPayload(Review review, List<UserError> userErrors) {}

    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) { this.reviews = reviews; }

    @MutationMapping(MUTATION.CreateReview)
    public CreateReviewPayload createReview(@Argument(MUTATION.CREATEREVIEW_INPUT_ARGUMENT.Input) CreateReviewInput input,
                                            @ContextValue(name = "viewer", required = false) String viewer) {
        var result = reviews.create(viewer, input.getBookId(), input.getRating(), input.getBody());
        var errors = result.problems().stream()
            .map(p -> UserError.newBuilder().field(p.field()).message(p.message()).build()).toList();
        return new CreateReviewPayload(result.review(), errors);
    }

    @MutationMapping(MUTATION.DeleteReview)
    public String deleteReview(@Argument(MUTATION.DELETEREVIEW_INPUT_ARGUMENT.Id) String id,
                               @ContextValue(name = "viewer", required = false) String viewer) {
        return reviews.delete(viewer, id);
    }

    @SubscriptionMapping(SUBSCRIPTION.ReviewAdded)
    public Flux<Review> reviewAdded(@Argument(SUBSCRIPTION.REVIEWADDED_INPUT_ARGUMENT.BookId) String bookId) { return reviews.added(bookId); }
}
