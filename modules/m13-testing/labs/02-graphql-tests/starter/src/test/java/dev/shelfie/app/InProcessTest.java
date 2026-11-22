package dev.shelfie.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;

/**
 * The whole app in-process. Add, next to this class: a test through the web layer (HttpGraphQlTester),
 * a subscription test on a real port (WebSocketGraphQlTester), and in dgs/ a DgsQueryExecutor test.
 * Keep documents in src/test/resources/graphql-test and load them with documentName.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
class InProcessTest {

    @Autowired ExecutionGraphQlServiceTester tester;

    @Test
    void hello() {
        tester.documentName("hello").execute().path("hello").entity(String.class).isEqualTo("Hello from Shelfie");
    }
}
