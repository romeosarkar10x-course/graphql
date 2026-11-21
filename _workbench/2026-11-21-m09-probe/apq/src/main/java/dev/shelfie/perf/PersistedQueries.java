package dev.shelfie.perf;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.ExecutionInput;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.persisted.ApolloPersistedQuerySupport;
import graphql.execution.preparsed.persisted.PersistedQueryCache;
import graphql.execution.preparsed.persisted.PersistedQueryCacheMiss;
import graphql.execution.preparsed.persisted.PersistedQueryNotFound;
import graphql.execution.preparsed.persisted.PersistedQuerySupport;
import java.util.concurrent.CompletableFuture;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PersistedQueries {
    static final Cache<Object, PreparsedDocumentEntry> CACHE = Caffeine.newBuilder().maximumSize(1_000).recordStats().build();

    static class CaffeineCache implements PersistedQueryCache {
        @Override
        public CompletableFuture<PreparsedDocumentEntry> getPersistedQueryDocumentAsync(Object id, ExecutionInput input,
                PersistedQueryCacheMiss onMiss) throws PersistedQueryNotFound {
            var hit = CACHE.getIfPresent(id);
            if (hit != null) return CompletableFuture.completedFuture(hit);
            String q = input.getQuery();
            if (q == null || q.isBlank() || q.equals(PersistedQuerySupport.PERSISTED_QUERY_MARKER)) throw new PersistedQueryNotFound(id);
            var entry = onMiss.apply(q);
            CACHE.put(id, entry);
            return CompletableFuture.completedFuture(entry);
        }
    }

    @Bean GraphQlSourceBuilderCustomizer apq() {
        return b -> b.configureGraphQl(g -> g.preparsedDocumentProvider(new ApolloPersistedQuerySupport(new CaffeineCache())));
    }
}
