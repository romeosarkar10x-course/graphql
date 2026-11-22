package dev.shelfie.app;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shelfie.app.store.ReviewRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;

/**
 * The whole app, in-process, through ExecutionGraphQlService: no HTTP, so no WebGraphQlInterceptor either
 * (the viewer is set on the execution input directly). Documents come from src/test/resources/graphql-test.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
class InProcessTest {

    @Autowired ExecutionGraphQlServiceTester tester;
    @Autowired ReviewRepository reviews;

    @BeforeEach
    void reset() { reviews.reset(); }

    ExecutionGraphQlServiceTester as(String viewer) {
        return tester.mutate()
            .configureExecutionInput((input, builder) -> builder.graphQLContext(Map.of("viewer", viewer)).build())
            .build();
    }

    @Test
    void clubPageFromADocumentFile() {
        tester.documentName("clubPage").variable("id", "c1").execute()
            .path("club.name").entity(String.class).isEqualTo("The Midnight Readers")
            .path("club.owner.name").entity(String.class).isEqualTo("Kemi Adeyemi")
            .path("club.memberCount").entity(Integer.class).isEqualTo(8)
            .path("club.members[*].handle").entityList(String.class).containsExactly("kemia", "asha", "benok")
            .path("club.currentBook.averageRating").entity(Double.class).isEqualTo(4.2)
            .path("club.currentBook.reviews[*].author.handle").entityList(String.class).containsExactly("hugos", "jonasb");
    }

    @Test
    void anUnknownReviewIsANotFoundErrorAtItsPath() {
        tester.documentName("review").variable("id", "zz").execute()
            .errors()
            .expect(e -> e.getErrorType() == ErrorType.NOT_FOUND && e.getPath().equals("review")
                && e.getMessage().equals("No review zz"))
            .verify()
            .path("review").valueIsNull();
    }

    @Test
    void deletingSomeoneElsesReviewIsForbiddenAtItsPath() {
        as("u2").documentName("deleteReview").variable("id", "r1").execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);
                assertThat(errors.getFirst().getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
                assertThat(errors.getFirst().getParsedPath()).containsExactly("deleteReview");
                assertThat(errors.getFirst().getLocations()).isNotEmpty();
            });
    }

    @Test
    void ruleViolationsAreDataNotErrors() {
        as("u5").documentName("createReview")
            .variable("input", Map.of("bookId", "b1", "rating", 9, "body", " "))
            .execute()
            .errors().verify()
            .path("createReview.review").valueIsNull()
            .path("createReview.userErrors[*].field").entityList(List.class)
            .containsExactly(List.of("input", "rating"), List.of("input", "body"));
    }

    @Test
    void aStoredReviewComesBackWithItsAuthor() {
        as("u5").documentName("createReview")
            .variable("input", Map.of("bookId", "b2", "rating", 4, "body", "  Halls!  "))
            .execute()
            .path("createReview.userErrors").entityList(Object.class).hasSize(0)
            .path("createReview.review.body").entity(String.class).isEqualTo("Halls!")
            .path("createReview.review.author.id").entity(String.class).isEqualTo("u5");
    }

    @Test
    void aBookWithoutReviewsHasANullAverage() {
        tester.document("{ book(id: \"b5\") { averageRating reviews { id } } }").execute()
            .path("book.averageRating").valueIsNull()
            .path("book.reviews").entityList(Object.class).hasSize(0);
    }
}
