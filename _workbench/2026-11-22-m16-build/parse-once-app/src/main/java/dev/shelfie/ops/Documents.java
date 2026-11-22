package dev.shelfie.ops;

import graphql.ExecutionInput;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.language.Document;
import graphql.parser.Parser;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Capacity experiment (16.6): parse and validate each distinct document once, not three times per request. */
@Configuration
public class Documents {
    static final Map<String, Document> PARSED = new ConcurrentHashMap<>();
    static final Map<String, PreparsedDocumentEntry> VALIDATED = new ConcurrentHashMap<>();

    static Document parse(String query) {
        if (PARSED.size() > 1000) PARSED.clear();
        return PARSED.computeIfAbsent(query, Parser::parse);
    }

    @Bean
    GraphQlSourceBuilderCustomizer preparsed() {
        PreparsedDocumentProvider provider = new PreparsedDocumentProvider() {
            @Override
            public CompletableFuture<PreparsedDocumentEntry> getDocumentAsync(ExecutionInput input,
                    Function<ExecutionInput, PreparsedDocumentEntry> parseAndValidate) {
                if (VALIDATED.size() > 1000) VALIDATED.clear();
                return CompletableFuture.completedFuture(VALIDATED.computeIfAbsent(input.getQuery(), q -> parseAndValidate.apply(input)));
            }
        };
        return builder -> builder.configureGraphQl(graphQl -> graphQl.preparsedDocumentProvider(provider));
    }
}
