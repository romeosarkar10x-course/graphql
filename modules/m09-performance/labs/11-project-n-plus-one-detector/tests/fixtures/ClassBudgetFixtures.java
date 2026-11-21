package dev.shelfie.perf.fixtures;

import dev.shelfie.perf.Gql;
import dev.shelfie.perf.detector.MaxStatements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/** A class-level budget, method overrides, @BeforeEach not counted, and a @Nested class inheriting it. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MaxStatements(2)
class ClassBudgetFixtures {
    @Value("${local.server.port}") int port;

    @BeforeEach void setUp() { Gql.post(port, BudgetFixtures.TEN_CLUBS); }   // 10 statements, outside the test method

    @Test void withinClassBudget() { Gql.post(port, "{ club(id: 1) { name } }"); }

    @Test @MaxStatements(20) void methodOverridesClass() { Gql.post(port, BudgetFixtures.TEN_CLUBS); }

    @Test void overClassBudget() { Gql.post(port, BudgetFixtures.TEN_CLUBS); }

    @Nested
    class Inner {
        @Test void inheritsOuterBudget() { Gql.post(port, BudgetFixtures.TEN_CLUBS); }
    }
}
