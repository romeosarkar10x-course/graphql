package dev.shelfie.perf;

import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * HTTP caching for GET. Spring for GraphQL sends no Cache-Control at all, and Varnish then caches every
 * GET for its default 120 s, personal or not. Anonymous, error-free responses may be shared for 60 s;
 * anything sent with X-Shelfie-User is private and never stored.
 */
@Component
@Order(1)
class HttpCaching implements WebGraphQlInterceptor {
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        boolean get = request.getUri().getQueryParams().containsKey("query") || request.getUri().getQueryParams().containsKey("extensions");
        boolean personal = request.getHeaders().getFirst("X-Shelfie-User") != null;
        return chain.next(request).doOnNext(response -> {
            if (!get) return;
            response.getResponseHeaders().set("Cache-Control",
                personal || !response.getErrors().isEmpty() ? "private, no-store" : "public, max-age=60");
            response.getResponseHeaders().set("Vary", "X-Shelfie-User");
        });
    }
}
