package dev.shelfie.perf;

import java.util.Map;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Puts the viewer (X-Shelfie-User) in the GraphQLContext. */
@Component
class RequestInterceptor implements WebGraphQlInterceptor {
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String viewer = request.getHeaders().getFirst("X-Shelfie-User");
        if (viewer != null && viewer.matches("[0-9]{1,9}"))
            request.configureExecutionInput((input, b) -> b.graphQLContext(Map.of("viewer", Long.parseLong(viewer))).build());
        return chain.next(request);
    }
}
