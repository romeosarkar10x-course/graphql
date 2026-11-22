package dev.shelfie.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration tests with real dependencies. PostgreSQL is wired for you; add Kafka (@ServiceConnection works
 * for it too) and Keycloak (KeycloakContainer, the course realm is in src/test/resources/keycloak; its issuer
 * goes in spring.security.oauth2.resourceserver.jwt.issuer-uri). Use the same images as the course services:
 * postgres:18.7-alpine, apache/kafka:4.4.0, quay.io/keycloak/keycloak:26.8.2.
 */
@SpringBootTest
@Testcontainers
class ShelfieIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.7-alpine");

    @Test
    void startsAgainstARealDatabase() {
        // TODO the SQL's order and rounding, a signed createReview stored and announced on Kafka, no token refused
    }
}
