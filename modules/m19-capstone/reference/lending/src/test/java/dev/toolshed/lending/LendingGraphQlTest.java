package dev.toolshed.lending;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Lending's GraphQL layer on a real PostgreSQL (Testcontainers; skipped without Docker), with the
 * authentication Spring Security would have made from a token. Runs the interceptor and the field rules.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class LendingGraphQlTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.7-alpine");

    @Autowired
    WebGraphQlHandler handler;

    WebGraphQlTester tester() {
        return WebGraphQlTester.create(handler);
    }

    static void signIn(String handle, String... roles) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").claim("preferred_username", handle)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, authorities, handle));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousCallersHaveNoMeAndCannotBorrow() {
        tester().document("{ me { id } }").execute().path("me").valueIsNull();
        tester().document("mutation { borrowTool(input: {toolId: \"t03\", idempotencyKey: \"a\"}) { __typename } }").execute()
                .errors().satisfy(errors -> assertThat(errors).first()
                        .satisfies(e -> assertThat(e.getErrorType().toString()).isEqualTo("UNAUTHORIZED")));
    }

    @Test
    void aRepeatedKeyReturnsTheFirstLoan() {
        signIn("ben");
        String borrow = "mutation { borrowTool(input: {toolId: \"t16\", days: 2, idempotencyKey: \"twice\"}) { ... on Borrowed { loan { id } } } }";
        String first = tester().document(borrow).execute().path("borrowTool.loan.id").entity(String.class).get();
        String second = tester().document(borrow).execute().path("borrowTool.loan.id").entity(String.class).get();
        assertThat(second).isEqualTo(first);
        tester().document("{ me { loans(status: OPEN) { edges { node { tool { id } } } } } }").execute()
                .path("me.loans.edges[*].node.tool.id").entityList(String.class).satisfies(ids -> assertThat(ids).containsOnlyOnce("t16"));
    }

    @Test
    void anOutToolIsATypedResult() {
        signIn("olu");
        tester().document("mutation { borrowTool(input: {toolId: \"t05\", idempotencyKey: \"x\"}) { __typename ... on ToolUnavailable { tool { id } } } }")
                .execute().path("borrowTool.__typename").entity(String.class).isEqualTo("ToolUnavailable")
                .path("borrowTool.tool.id").entity(String.class).isEqualTo("t05");
    }

    @Test
    void emailIsForTheMemberAndLibrariansOnly() {
        signIn("ada");
        tester().document("{ me { email } }").execute().path("me.email").entity(String.class).isEqualTo("ada@example.org");
        String desk = "{ _entities(representations: [{__typename: \"Tool\", id: \"t10\"}]) { ... on Tool { currentLoan { member { email } } } } }";
        tester().document(desk).execute().errors().satisfy(errors -> assertThat(errors).first()
                .satisfies(e -> assertThat(e.getErrorType().toString()).isEqualTo("FORBIDDEN")));
        signIn("root", "admin");
        tester().document(desk).execute().path("_entities[0].currentLoan.member.email").entity(String.class).isEqualTo("ben@example.org");
    }

    @Test
    void theOverdueReportIsForLibrarians() {
        signIn("max");
        tester().document("{ overdueLoans { edges { node { id } } } }").execute().errors()
                .satisfy(errors -> assertThat(errors).first().satisfies(e -> assertThat(e.getMessage()).isEqualTo("librarians only")));
        signIn("root", "admin");
        tester().document("{ overdueLoans { edges { node { id status } } } }").execute()
                .path("overdueLoans.edges[*].node.status").entityList(String.class).satisfies(s -> assertThat(s).containsOnly("OVERDUE"));
    }
}
