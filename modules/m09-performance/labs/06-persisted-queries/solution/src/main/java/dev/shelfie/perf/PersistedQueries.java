package dev.shelfie.perf;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.ExecutionInput;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.persisted.ApolloPersistedQuerySupport;
import graphql.execution.preparsed.persisted.PersistedQueryCache;
import graphql.execution.preparsed.persisted.PersistedQueryCacheMiss;
import graphql.execution.preparsed.persisted.PersistedQueryIdInvalid;
import graphql.execution.preparsed.persisted.PersistedQueryNotFound;
import graphql.execution.preparsed.persisted.PersistedQuerySupport;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Automatic persisted queries: graphql-java's ApolloPersistedQuerySupport, with the parsed and validated
 * documents in Caffeine. A hit skips parsing and validation as well as the query text on the wire.
 */
@Configuration
class PersistedQueries {
    static final Cache<Object, PreparsedDocumentEntry> DOCUMENTS = Caffeine.newBuilder().maximumSize(1_000).recordStats().build();

    static class CaffeinePersistedQueryCache implements PersistedQueryCache {
        @Override
        public CompletableFuture<PreparsedDocumentEntry> getPersistedQueryDocumentAsync(Object id, ExecutionInput input,
                PersistedQueryCacheMiss onMiss) throws PersistedQueryNotFound {
            String text = input.getQuery();
            boolean hasText = text != null && !text.isBlank() && !text.equals(PersistedQuerySupport.PERSISTED_QUERY_MARKER);
            var hit = DOCUMENTS.getIfPresent(id);
            if (hit != null) {
                // graphql-java checks the hash only on a miss. A known hash with other text would run the
                // cached document and ignore the text the client sent: refuse that instead.
                if (hasText && !sha256(text).equals(id)) throw new PersistedQueryIdInvalid(id);
                return CompletableFuture.completedFuture(hit);
            }
            if (!hasText) throw new PersistedQueryNotFound(id);
            var entry = onMiss.apply(text);   // checks the hash, then parses and validates
            DOCUMENTS.put(id, entry);
            return CompletableFuture.completedFuture(entry);
        }
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    @Bean GraphQlSourceBuilderCustomizer apqCustomizer() {
        return b -> b.configureGraphQl(g -> g.preparsedDocumentProvider(new ApolloPersistedQuerySupport(new CaffeinePersistedQueryCache())));
    }
}
