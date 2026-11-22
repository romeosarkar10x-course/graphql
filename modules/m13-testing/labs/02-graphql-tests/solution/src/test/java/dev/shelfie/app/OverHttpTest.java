package dev.shelfie.app;

import dev.shelfie.app.store.ReviewRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * Through the web layer (MockMvc, no real port): the X-Shelfie-User header goes through ViewerInterceptor,
 * which the in-process tester skips.
 */
@SpringBootTest
@AutoConfigureHttpGraphQlTester
class OverHttpTest {

    @Autowired HttpGraphQlTester http;
    @Autowired ReviewRepository reviews;

    @BeforeEach
    void reset() { reviews.reset(); }

    @Test
    void theHeaderBecomesTheViewer() {
        http.mutate().header("X-Shelfie-User", "u11").build()
            .document("{ viewer { name clubs { id } } }").execute()
            .path("viewer.name").entity(String.class).isEqualTo("Kemi Adeyemi")
            .path("viewer.clubs[*].id").entityList(String.class).containsExactly("c1");
    }

    @Test
    void withoutTheHeaderThereIsNoViewer() {
        http.document("{ viewer { name } }").execute().path("viewer").valueIsNull();
    }

    @Test
    void theHeaderSignsTheMutation() {
        http.mutate().header("X-Shelfie-User", "u6").build()
            .documentName("createReview").variable("input", Map.of("bookId", "b3", "rating", 5, "body", "Stevens!"))
            .execute()
            .path("createReview.review.author.id").entity(String.class).isEqualTo("u6");
    }

    @Test
    void theAuthorMayDeleteOverHttp() {
        http.mutate().header("X-Shelfie-User", "u1").build()
            .documentName("deleteReview").variable("id", "r1").execute()
            .path("deleteReview").entity(String.class).isEqualTo("r1");
    }
}
