package dev.shelfie.perf;

import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** HTTP caching for GET: let browsers and the CDN keep responses for a minute. */
@Component
@Order(1)
class HttpCaching implements WebGraphQlInterceptor {
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        boolean get = request.getUri().getQueryParams().containsKey("query") || request.getUri().getQueryParams().containsKey("extensions");
        return chain.next(request).doOnNext(response -> {
            if (get) response.getResponseHeaders().set("Cache-Control", "public, max-age=60");
        });
    }
}
