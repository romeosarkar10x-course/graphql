package dev.shelfie.gateway;

import dev.shelfie.gateway.Cache.Lookup;
import dev.shelfie.gateway.OpenLibrary.Edition;
import dev.shelfie.gateway.OpenLibrary.Search;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/** Every edition goes through one DataLoader per request, so aliases and lists share one Open Library call. */
@Controller
public class GatewayController {
    private final Cache cache;

    GatewayController(Cache cache, BatchLoaderRegistry registry) {
        this.cache = cache;
        registry.<String, Lookup>forName("editions").registerMappedBatchLoader((isbns, env) -> Mono.fromCallable(() -> cache.editions(isbns)));
    }

    @QueryMapping
    public CompletableFuture<DataFetcherResult<Edition>> book(@Argument String isbn, DataLoader<String, Lookup> editions, DataFetchingEnvironment env) {
        return editions.load(isbn).thenApply(l -> {
            var r = DataFetcherResult.<Edition>newResult().data(l == null ? null : l.edition());
            if (l != null && l.error() != null) r.error(unavailable(env, l.error(), null));
            return r.build();
        });
    }

    @QueryMapping
    public CompletableFuture<DataFetcherResult<List<Edition>>> books(@Argument List<String> isbns, DataLoader<String, Lookup> editions, DataFetchingEnvironment env) {
        return editions.loadMany(isbns).thenApply(lookups -> {
            List<Edition> data = new ArrayList<>();
            var r = DataFetcherResult.<List<Edition>>newResult();
            for (int i = 0; i < lookups.size(); i++) {
                Lookup l = lookups.get(i);
                data.add(l == null ? null : l.edition());
                if (l != null && l.error() != null) r.error(unavailable(env, l.error(), i));
            }
            return r.data(data).build();
        });
    }

    @QueryMapping public Search search(@Argument String text, @Argument int first) { return cache.search(text, Math.max(0, Math.min(first, 100))); }

    @SchemaMapping public List<String> subjects(Edition e, @Argument int first) { return e.subjects().stream().limit(Math.max(0, first)).toList(); }
    @SchemaMapping public String fetchedAt(Edition e) { return e.fetchedAt().toString(); }

    static GraphQLError unavailable(DataFetchingEnvironment env, String message, Integer index) {
        var b = GraphqlErrorBuilder.newError(env).errorType(ErrorType.INTERNAL_ERROR).message(message)
            .extensions(Map.of("code", "UPSTREAM_UNAVAILABLE"));
        if (index != null) b.path(env.getExecutionStepInfo().getPath().segment(index));
        return b.build();
    }

    @GraphQlExceptionHandler
    GraphQLError searchUnavailable(OpenLibrary.Unavailable e, DataFetchingEnvironment env) {
        return unavailable(env, e.getMessage(), null);
    }
}
