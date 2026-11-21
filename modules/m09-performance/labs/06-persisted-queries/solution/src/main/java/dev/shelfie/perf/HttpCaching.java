package dev.shelfie.perf;

import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Cache-Control for GET. Spring for GraphQL sets none, and Varnish then keeps every GET for 120 s,
 * including a PersistedQueryNotFound: the client registers the query by POST, and the next GET is still
 * the cached error. Only anonymous, error-free responses may be shared.
 */
@Component
@Order(1)
class HttpCaching implements WebGraphQlInterceptor {
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        var params = request.getUri().getQueryParams();
        boolean get = params.containsKey("query") || params.containsKey("extensions");
        boolean personal = request.getHeaders().getFirst("X-Shelfie-User") != null;
        return chain.next(request).doOnNext(response -> {
            if (!get) return;
            response.getResponseHeaders().set("Cache-Control",
                personal || !response.getErrors().isEmpty() ? "private, no-store" : "public, max-age=60");
            response.getResponseHeaders().set("Vary", "X-Shelfie-User");
        });
    }
}
