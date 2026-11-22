package dev.shelfie.tenancy;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * The tenant comes from the verified token, never from a header or an argument: anything the client can type,
 * an attacker can type. Runs on the request thread, where Spring Security's context is set.
 */
@Component
public class TenantInterceptor implements WebGraphQlInterceptor {
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
            String tenant = auth.getToken().getClaimAsString("tenant");
            if (tenant != null && !tenant.isBlank()) {
                Tenant t = new Tenant(auth.getName(), tenant);
                request.configureExecutionInput((input, builder) -> builder.graphQLContext(c -> c.put(Tenant.KEY, t)).build());
            }
        }
        return chain.next(request);
    }
}
