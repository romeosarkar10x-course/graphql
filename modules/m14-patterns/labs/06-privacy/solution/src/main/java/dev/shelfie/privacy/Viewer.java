package dev.shelfie.privacy;

import graphql.GraphQLContext;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** The signed-in handle and whether they are an admin, from the verified token, in the GraphQLContext. */
public record Viewer(String handle, boolean admin) {
    static final String KEY = "viewer";

    static Viewer of(GraphQLContext context) {
        return context.get(KEY);
    }

    @Component
    static class Interceptor implements WebGraphQlInterceptor {
        @Override
        public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
            if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
                boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_admin"));
                Viewer v = new Viewer(auth.getName(), admin);
                request.configureExecutionInput((input, builder) -> builder.graphQLContext(c -> c.put(KEY, v)).build());
            }
            return chain.next(request);
        }
    }
}
