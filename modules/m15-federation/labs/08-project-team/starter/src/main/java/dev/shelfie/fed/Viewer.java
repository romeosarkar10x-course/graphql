package dev.shelfie.fed;

import java.util.Map;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** The signed-in reader, from the X-Shelfie-User header the router forwards (or does not): context "userId". */
@Component
public class Viewer implements WebGraphQlInterceptor {
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String user = request.getHeaders().getFirst("X-Shelfie-User");
        if (user != null && Data.USERS.containsKey(user)) {
            request.configureExecutionInput((input, builder) -> builder.graphQLContext(Map.of("userId", user)).build());
        }
        return chain.next(request);
    }
}
