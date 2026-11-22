package dev.shelfie.app.graph;

import java.util.Map;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;

/** Sets GraphQLContext's "viewer" the way ViewerInterceptor would, for testers that skip the web layer. */
final class ViewerContext {
    private ViewerContext() {}

    static GraphQlTester with(String viewer, GraphQlTester tester) {
        return ((ExecutionGraphQlServiceTester) tester).mutate()
            .configureExecutionInput((input, builder) -> builder.graphQLContext(Map.of("viewer", viewer)).build())
            .build();
    }
}
