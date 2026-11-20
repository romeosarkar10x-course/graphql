package dev.shelfie.ref.graphql;

import java.util.Map;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Module 4 stand-in for authentication: the {@code X-Shelfie-User} header names the acting user and
 * lands in the GraphQL context as {@code viewer}. Real authentication arrives in Module 10.
 */
@Component
public class ViewerInterceptor implements WebGraphQlInterceptor {
    public static final String HEADER = "X-Shelfie-User";

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String viewer = request.getHeaders().getFirst(HEADER);
        if (viewer != null && !viewer.isBlank()) {
            request.configureExecutionInput((input, builder) -> builder.graphQLContext(Map.of("viewer", viewer.trim())).build());
        }
        return chain.next(request);
    }
}
