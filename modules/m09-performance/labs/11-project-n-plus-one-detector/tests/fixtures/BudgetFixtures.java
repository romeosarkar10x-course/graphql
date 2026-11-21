package dev.shelfie.perf.fixtures;

import dev.shelfie.perf.Gql;
import dev.shelfie.perf.detector.MaxStatements;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/** The harness's fixtures for the N+1 detector: some of these must fail, with the right message. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BudgetFixtures {
    static final String TEN_CLUBS = "{ a: club(id: 1) { name } b: club(id: 2) { name } c: club(id: 3) { name } d: club(id: 4) { name } "
        + "e: club(id: 5) { name } f: club(id: 6) { name } g: club(id: 7) { name } h: club(id: 8) { name } i: club(id: 9) { name } j: club(id: 10) { name } }";

    @Value("${local.server.port}") int port;

    String dashboard() throws Exception { return Files.readString(Path.of("ci/dashboard.graphql")); }

    @Test @MaxStatements(9) void withinBudget() throws Exception { Gql.post(port, dashboard()); }

    @Test @MaxStatements(5) void overBudget() throws Exception { Gql.post(port, dashboard()); }

    @Test @MaxStatements(3) void nPlusOne() { Gql.post(port, TEN_CLUBS); }

    @Test void unlimited() { Gql.post(port, TEN_CLUBS); }

    @Test @MaxStatements(100) void ownFailure() { throw new AssertionError("the test's own failure"); }
}
