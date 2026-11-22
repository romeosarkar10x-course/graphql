package dev.shelfie.app.graph;

import dev.shelfie.app.service.CatalogService;
import dev.shelfie.app.service.ReviewService;
import dev.shelfie.app.store.CatalogRepository;
import dev.shelfie.app.store.ReviewRepository;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

/**
 * The GraphQL slice: schema, controllers and advice, no web server. Real services over the real seed, so the
 * test checks what the controllers pass through (arguments, the viewer), not the services' rules again.
 * The Clock comes from ShelfieApplication: a slice still reads @Bean methods on the @SpringBootApplication class.
 */
@GraphQlTest({QueryController.class, ClubController.class, BookController.class, ReviewController.class, ErrorAdvice.class})
@Import(ShelfieControllerSliceTest.Wiring.class)
class ShelfieControllerSliceTest {

    @TestConfiguration
    static class Wiring {
        @Bean CatalogRepository catalogRepository() { return new CatalogRepository(); }
        @Bean ReviewRepository reviewRepository() { return new ReviewRepository(); }
        @Bean CatalogService catalogService(CatalogRepository c) { return new CatalogService(c); }
        @Bean ReviewService reviewService(ReviewRepository r, CatalogRepository c, Clock clock) { return new ReviewService(r, c, clock); }
    }

    @Autowired GraphQlTester tester;
    @Autowired ReviewRepository reviews;

    @BeforeEach
    void reset() { reviews.reset(); }


    @Test
    void booksPassesTheFilterAndTheCapThrough() {
        tester.document("{ books(titleContains: \"the\", first: 3) { id } }").execute()
            .path("books[*].id").entityList(String.class).containsExactly("b1", "b3", "b4");
    }

    @Test
    void booksDefaultsFirstToTwenty() {
        tester.document("{ books { id } }").execute().path("books").entityList(Object.class).hasSize(10);
    }

    @Test
    void membersArgumentReachesTheService() {
        tester.document("{ club(id: \"c1\") { members(first: 2) { handle } memberCount } }").execute()
            .path("club.members[*].handle").entityList(String.class).containsExactly("kemia", "asha")
            .path("club.memberCount").entity(Integer.class).isEqualTo(8);
    }

    @Test
    void anUnknownClubIsNullWithoutErrors() {
        tester.document("{ club(id: \"zz\") { name } }").execute().errors().verify().path("club").valueIsNull();
    }

    @Test
    void withoutAViewerCreateReviewIsRefused() {
        tester.document("mutation { createReview(input: {bookId: \"b2\", rating: 5, body: \"Hi\"}) { review { id } userErrors { field message } } }")
            .execute()
            .path("createReview.review").valueIsNull()
            .path("createReview.userErrors[0].message").entity(String.class).isEqualTo("Sign in to review (X-Shelfie-User header)");
    }

    @Test
    void theViewerFromTheContextIsTheAuthor() {
        ViewerContext.with("u5", tester)
            .document("mutation { createReview(input: {bookId: \"b2\", rating: 5, body: \"Hi\"}) { review { id author { id } } userErrors { message } } }")
            .execute()
            .path("createReview.userErrors").entityList(Object.class).hasSize(0)
            .path("createReview.review.author.id").entity(String.class).isEqualTo("u5");
    }

    @Test
    void userErrorsCarryTheInputPath() {
        ViewerContext.with("u5", tester)
            .document("mutation { createReview(input: {bookId: \"b2\", rating: 0, body: \"Hi\"}) { userErrors { field } } }")
            .execute()
            .path("createReview.userErrors[0].field").entityList(String.class).containsExactly("input", "rating");
    }

    @Test
    void averageRatingAndReviewsComeFromTheService() {
        tester.document("{ book(id: \"b1\") { averageRating reviews(last: 2) { id } } b5: book(id: \"b5\") { averageRating } }").execute()
            .path("book.averageRating").entity(Double.class).isEqualTo(4.2)
            .path("book.reviews[*].id").entityList(String.class).containsExactly("r5", "r4")
            .path("b5.averageRating").valueIsNull();
    }

    @Test
    void someoneElsesReviewCannotBeDeleted() {
        ViewerContext.with("u2", tester).document("mutation { deleteReview(id: \"r1\") }").execute()
            .errors().expect(e -> e.getErrorType().toString().equals("FORBIDDEN")).verify();
    }
}
