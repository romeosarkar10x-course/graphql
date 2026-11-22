package dev.shelfie.it;

import static org.assertj.core.api.Assertions.assertThat;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real dependencies, started by Testcontainers for this test run and thrown away after it: PostgreSQL
 * (the SQL really runs), Kafka (the event really leaves), Keycloak with the course realm (tokens really
 * verify against its keys). Same images as the course services, so nothing extra to download.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ShelfieIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.7-alpine");

    @Container @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.4.0");

    @Container
    static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.8.2")
        .withRealmImportFile("/keycloak/shelfie-realm.json");

    @DynamicPropertySource
    static void issuer(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> keycloak.getAuthServerUrl() + "/realms/shelfie");
    }

    static KafkaConsumer<String, String> events;

    @BeforeAll
    static void listen() {
        events = new KafkaConsumer<>(Map.of(
            "bootstrap.servers", kafka.getBootstrapServers(), "group.id", "it-" + UUID.randomUUID(),
            "auto.offset.reset", "earliest", "key.deserializer", StringDeserializer.class.getName(),
            "value.deserializer", StringDeserializer.class.getName()));
        events.subscribe(List.of("shelfie.reviews"));
    }

    @AfterAll
    static void close() { events.close(); }

    @LocalServerPort int port;
    HttpGraphQlTester graphql;

    /** Over a real port, so the request goes through Spring Security's filter chain like a client's would. */
    @org.junit.jupiter.api.BeforeEach
    void client() { graphql = HttpGraphQlTester.create(WebTestClient.bindToServer().baseUrl("http://localhost:" + port + "/graphql").build()); }

    /** A real access token: the password grant against Keycloak, as the course's shelfie-cli client. */
    static String token(String user) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", "shelfie-cli");
        form.add("username", user);
        form.add("password", user + "-pw");
        Map<?, ?> body = RestClient.create().post()
            .uri(keycloak.getAuthServerUrl() + "/realms/shelfie/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);
        return (String) body.get("access_token");
    }

    HttpGraphQlTester as(String user) { return graphql.mutate().header("Authorization", "Bearer " + token(user)).build(); }

    static final String CREATE = "mutation($b: ID!, $r: Int!) { createReview(input: {bookId: $b, rating: $r, body: \" Hello \"}) { id rating body author } }";

    List<ConsumerRecord<String, String>> eventsFor(String bookId) {
        var seen = new ArrayList<ConsumerRecord<String, String>>();
        long until = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (seen.isEmpty() && System.nanoTime() < until)
            events.poll(Duration.ofMillis(250)).forEach(r -> { if (bookId.equals(r.key())) seen.add(r); });
        return seen;
    }

    @Test
    void averageIsRoundedByPostgres() {
        graphql.document("{ book(id: \"b2\") { averageRating } b5: book(id: \"b5\") { averageRating } }").execute()
            .path("book.averageRating").entity(Double.class).isEqualTo(4.33)
            .path("b5.averageRating").valueIsNull();
    }

    @Test
    void reviewsAreNewestFirst() {
        graphql.document("{ book(id: \"b1\") { reviews(last: 2) { author } } }").execute()
            .path("book.reviews[*].author").entityList(String.class).containsExactly("u3", "u2");
    }

    @Test
    void aSignedReviewIsStoredAndAnnounced() {
        as("ada").document(CREATE).variable("b", "b3").variable("r", 4).execute()
            .path("createReview.author").entity(String.class).isEqualTo("ada")
            .path("createReview.body").entity(String.class).isEqualTo("Hello");
        graphql.document("{ book(id: \"b3\") { averageRating reviews { author } } }").execute()
            .path("book.averageRating").entity(Double.class).isEqualTo(4.0)
            .path("book.reviews[0].author").entity(String.class).isEqualTo("ada");
        var announced = eventsFor("b3");
        assertThat(announced).hasSize(1);
        assertThat(announced.getFirst().value()).contains("\"author\":\"ada\"").contains("\"rating\":4");
    }

    @Test
    void theDatabaseRefusesASecondReview() {
        as("ben").document(CREATE).variable("b", "b4").variable("r", 5).execute().errors().verify();
        as("ben").document(CREATE).variable("b", "b4").variable("r", 3).execute()
            .errors().expect(e -> e.getMessage().equals("You already reviewed this book")).verify();
    }

    @Test
    void withoutATokenThereIsNoAuthor() {
        graphql.document(CREATE).variable("b", "b5").variable("r", 4).execute()
            .errors().expect(e -> e.getErrorType() == ErrorType.UNAUTHORIZED).verify();
    }
}
