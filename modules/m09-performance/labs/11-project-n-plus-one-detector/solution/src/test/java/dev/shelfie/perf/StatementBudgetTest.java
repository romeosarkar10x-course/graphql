package dev.shelfie.perf;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shelfie.perf.detector.MaxStatements;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/** The app's statement budgets. A change that brings back an N+1 fails here, in CI, before production. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MaxStatements(3)
class StatementBudgetTest {
    @Value("${local.server.port}") int port;

    @Test
    @MaxStatements(9)
    void dashboard() throws Exception {
        String query = Files.readString(Path.of(System.getProperty("dashboard", "ci/dashboard.graphql")));
        var data = (Map<?, ?>) Gql.post(port, query).get("data");
        assertThat((List<?>) data.get("clubs")).hasSize(10);
    }

    @Test
    void oneClubWithOwnerAndMembers() {
        var r = Gql.post(port, "{ club(id: 1) { name owner { name } members(first: 3) { handle } } }");
        assertThat(r).doesNotContainKey("errors");
    }

    @Test
    void threeLevelsOfLists() {
        var r = Gql.post(port, "{ clubs(first: 3) { members(first: 2) { reviews(first: 2) { rating } } } }");
        assertThat(r).doesNotContainKey("errors");
    }

    @Test
    @MaxStatements(0)
    void viewerWithoutHeaderTouchesNothing() {
        assertThat(Gql.post(port, "{ viewer { handle } }")).doesNotContainKey("errors");
    }
}
