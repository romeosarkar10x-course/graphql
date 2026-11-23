package dev.toolshed.lending;

import java.util.Map;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Runs once per GraphQL request, after Spring Security verified the token (a bad token is 401 before this):
 * looks the member up by the token's preferred_username and puts the Caller in the GraphQL context.
 */
@Component
class CallerInterceptor implements WebGraphQlInterceptor {
    private final LendingRepository db;

    CallerInterceptor(LendingRepository db) {
        this.db = db;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Caller caller = auth instanceof JwtAuthenticationToken jwt
                ? new Caller(db.memberByHandle(jwt.getName()),
                        jwt.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_admin")))
                : Caller.ANONYMOUS;
        request.configureExecutionInput((input, builder) -> builder.graphQLContext(Map.of("caller", caller)).build());
        return chain.next(request);
    }
}
