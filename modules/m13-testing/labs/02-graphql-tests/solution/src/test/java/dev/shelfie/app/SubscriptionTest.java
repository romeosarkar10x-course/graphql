package dev.shelfie.app;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shelfie.app.service.ReviewService;
import dev.shelfie.app.store.ReviewRepository;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.graphql.test.tester.WebSocketGraphQlTester;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/** A real port: the subscription over graphql-transport-ws, the mutations over HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubscriptionTest {

    @LocalServerPort int port;
    @Autowired ReviewService service;
    @Autowired ReviewRepository reviews;

    @BeforeEach
    void reset() { reviews.reset(); }

    WebSocketGraphQlTester ws() {
        return WebSocketGraphQlTester.builder(URI.create("ws://localhost:" + port + "/graphql"), new ReactorNettyWebSocketClient()).build();
    }

    HttpGraphQlTester http(String viewer) {
        var client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port + "/graphql");
        return HttpGraphQlTester.builder(client).header("X-Shelfie-User", viewer).build();
    }

    void review(String viewer, String bookId, int rating) {
        http(viewer).documentName("createReview")
            .variable("input", Map.of("bookId", bookId, "rating", rating, "body", "Live from " + viewer))
            .execute().path("createReview.userErrors").entityList(Object.class).hasSize(0);
    }

    /** The feed is hot: publish only once the server has registered the subscription. */
    void awaitSubscribers(int n) {
        long until = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (service.liveSubscribers() < n && System.nanoTime() < until) Thread.onSpinWait();
        assertThat(service.liveSubscribers()).as("live subscriptions").isGreaterThanOrEqualTo(n);
    }

    @Test
    void exactlyThreeEventsInOrderForOneBook() {
        WebSocketGraphQlTester tester = ws();
        Flux<String> ids = tester.documentName("reviewAdded").variable("bookId", "b2")
            .executeSubscription().toFlux("reviewAdded.author.handle", String.class);

        StepVerifier.create(ids.take(Duration.ofSeconds(3)))
            .then(() -> {
                awaitSubscribers(1);
                review("u2", "b2", 5);
                review("u3", "b3", 4);   // another book: must not arrive
                review("u4", "b2", 3);
                review("u7", "b2", 2);
            })
            .expectNext("benok", "devp", "gracek")
            .verifyComplete();
        tester.stop().block(Duration.ofSeconds(2));
    }

    @Test
    void withoutABookIdEveryBookArrives() {
        WebSocketGraphQlTester tester = ws();
        Flux<String> books = tester.documentName("reviewAdded")
            .executeSubscription().toFlux("reviewAdded.book.id", String.class);

        StepVerifier.create(books.take(2))
            .then(() -> { awaitSubscribers(1); review("u2", "b2", 5); review("u3", "b3", 4); })
            .expectNext("b2", "b3")
            .expectComplete()
            .verify(Duration.ofSeconds(5));
        tester.stop().block(Duration.ofSeconds(2));
    }
}
