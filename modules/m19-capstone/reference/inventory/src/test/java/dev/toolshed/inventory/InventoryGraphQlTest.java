package dev.toolshed.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Metrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Inventory's GraphQL layer on a real PostgreSQL (Testcontainers; skipped without Docker), and the N+1 gate. */
@SpringBootTest
@AutoConfigureGraphQlTester
@Testcontainers(disabledWithoutDocker = true)
class InventoryGraphQlTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.7-alpine");

    @Autowired
    GraphQlTester graphQl;

    @Test
    void pagesDoNotOverlap() {
        String page = "query($after: String) { tools(first: 4, after: $after) { edges { node { id } } pageInfo { endCursor hasNextPage } } }";
        var first = graphQl.document(page).execute();
        String cursor = first.path("tools.pageInfo.endCursor").entity(String.class).get();
        var ids1 = first.path("tools.edges[*].node.id").entityList(String.class).get();
        var ids2 = graphQl.document(page).variable("after", cursor).execute().path("tools.edges[*].node.id").entityList(String.class).get();
        assertThat(ids1).hasSize(4).doesNotContainAnyElementsOf(ids2);
    }

    @Test
    void oneOfFindsByBarcode() {
        graphQl.document("{ tool(by: {barcode: \"TS-0024\"}) { name specs { __typename ... on PowerSpecs { volts } } } }").execute()
                .path("tool.name").entity(String.class).isEqualTo("Laser level")
                .path("tool.specs.__typename").entity(String.class).isEqualTo("PowerSpecs");
    }

    @Test
    void aCursorFromAnotherSortIsRefused() {
        String cursor = graphQl.document("{ tools(first: 1, sort: NEWEST) { pageInfo { endCursor } } }").execute()
                .path("tools.pageInfo.endCursor").entity(String.class).get();
        graphQl.document("query($c: String) { tools(first: 1, after: $c, sort: NAME_ASC) { totalCount } }").variable("c", cursor).execute()
                .errors().satisfy(e -> assertThat(e).first().satisfies(err -> assertThat(err.getMessage()).contains("cursor")));
    }

    /** The N+1 detector: a page of 30 runs as many statements as a page of 2. */
    @Test
    void statementsDoNotGrowWithThePage() {
        String page = "query($n: Int!) { tools(first: $n) { edges { node { name category { name toolCount } branch { name } } } } }";
        assertThat(statements(page, 30)).isEqualTo(statements(page, 2));
    }

    double statements(String document, int n) {
        double before = Metrics.globalRegistry.get("toolshed.db.statements").counter().count();
        graphQl.document(document).variable("n", n).executeAndVerify();
        return Metrics.globalRegistry.get("toolshed.db.statements").counter().count() - before;
    }
}
