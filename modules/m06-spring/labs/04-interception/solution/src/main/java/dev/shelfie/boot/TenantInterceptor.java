package dev.shelfie.boot;

import java.util.Map;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Request: X-Shelfie-Tenant into GraphQLContext. Response: which tenant was served, as a header and in extensions. */
@Component
public class TenantInterceptor implements WebGraphQlInterceptor {
    static final Map<String, String> ZONES = Map.of("eu", "Europe/Paris", "in", "Asia/Kolkata", "us", "America/New_York");

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String header = request.getHeaders().getFirst("X-Shelfie-Tenant");
        String tenant = header != null && ZONES.containsKey(header) ? header : "global";
        request.configureExecutionInput((input, builder) -> builder.graphQLContext(Map.of("tenant", tenant)).build());
        return chain.next(request).map(response -> {
            response.getResponseHeaders().set("X-Shelfie-Tenant-Served", tenant);
            return response.transform(b -> b.extensions(Map.of("tenant", tenant)));
        });
    }
}
