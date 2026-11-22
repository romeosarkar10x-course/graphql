package dev.shelfie.app.graph;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

/**
 * TODO make this a @GraphQlTest slice: the schema, the controllers and ErrorAdvice, with no web server.
 * The slice does not scan @Service or @Repository classes: provide them (a @TestConfiguration, or @Import).
 * Test what the controllers pass through: arguments, defaults from the schema, the viewer, errors.
 */
class ShelfieControllerSliceTest {

    @Autowired GraphQlTester tester;

    @Test
    void hello() {
        tester.document("{ hello }").execute().path("hello").entity(String.class).isEqualTo("Hello from Shelfie");
    }
}
