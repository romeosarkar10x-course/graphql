package dev.shelfie.tenancy;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Puts the tenant in the GraphQLContext. Support asked for X-Tenant so staff can look at any shop. */
@Component
public class TenantInterceptor implements WebGraphQlInterceptor {
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
            String header = request.getHeaders().getFirst("X-Tenant");
            String tenant = header != null ? header : auth.getToken().getClaimAsString("tenant");
            if (tenant != null && !tenant.isBlank()) {
                Tenant t = new Tenant(auth.getName(), tenant);
                request.configureExecutionInput((input, builder) -> builder.graphQLContext(c -> c.put(Tenant.KEY, t)).build());
            }
        }
        return chain.next(request);
    }
}
