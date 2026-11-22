package dev.shelfie.app.graph;

import java.util.Map;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Copies the X-Shelfie-User header into GraphQLContext as "viewer" (a stand-in for real authentication). */
@Component
public class ViewerInterceptor implements WebGraphQlInterceptor {
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String viewer = request.getHeaders().getFirst("X-Shelfie-User");
        if (viewer != null && !viewer.isBlank())
            request.configureExecutionInput((input, builder) -> builder.graphQLContext(Map.of("viewer", viewer)).build());
        return chain.next(request);
    }
}
