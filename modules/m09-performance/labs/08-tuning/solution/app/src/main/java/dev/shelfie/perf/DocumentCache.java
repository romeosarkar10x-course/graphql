package dev.shelfie.perf;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.ExecutionInput;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Parse and validate each distinct document once (JMH in lab 9.7: about 19 us of 23 us per dashboard
 * execution). Keyed by the exact text; bounded, because clients can send any number of documents.
 */
@Configuration
class DocumentCache {
    private final Cache<String, PreparsedDocumentEntry> documents = Caffeine.newBuilder().maximumSize(1_000).build();

    @Bean GraphQlSourceBuilderCustomizer documentCacheCustomizer() {
        return b -> b.configureGraphQl(g -> g.preparsedDocumentProvider(new PreparsedDocumentProvider() {
            @Override
            public CompletableFuture<PreparsedDocumentEntry> getDocumentAsync(ExecutionInput input,
                    Function<ExecutionInput, PreparsedDocumentEntry> parseAndValidate) {
                return CompletableFuture.completedFuture(documents.get(input.getQuery(), q -> parseAndValidate.apply(input)));
            }
        }));
    }
}
