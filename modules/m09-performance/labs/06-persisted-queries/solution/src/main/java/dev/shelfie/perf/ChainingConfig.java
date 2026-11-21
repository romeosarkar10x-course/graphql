package dev.shelfie.perf;

import graphql.execution.instrumentation.dataloader.DataLoaderDispatchingContextKeys;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * graphql-java 26 dispatches DataLoaders once per level. A load made after the level was dispatched (a
 * chained load, in thenCompose) waits forever unless chaining is enabled for the request. Neither
 * Spring for GraphQL 2.1 nor Boot 4.2 turns it on.
 */
@Component
class ChainingConfig implements WebGraphQlInterceptor {
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        request.configureExecutionInput((input, b) -> {
            DataLoaderDispatchingContextKeys.setEnableDataLoaderChaining(input.getGraphQLContext(), true);
            return input;
        });
        return chain.next(request);
    }
}
