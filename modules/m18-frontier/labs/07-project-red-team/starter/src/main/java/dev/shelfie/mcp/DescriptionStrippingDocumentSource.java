package dev.shelfie.mcp;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.graphql.support.DocumentSource;
import org.springframework.graphql.support.ResourceDocumentSource;
import reactor.core.publisher.Mono;

/**
 * Loads operations like Spring's ResourceDocumentSource, then removes their descriptions. The September 2025 edition
 * allows a description before an operation, a fragment or a variable; graphql-java 26.1 (Shelfie's engine) rejects
 * them, so they are documentation for the agent only and never travel to Shelfie.
 */
class DescriptionStrippingDocumentSource implements DocumentSource {
    static final Pattern DESCRIPTION = Pattern.compile(
            "(\"\"\"(?:[^\"\\\\]|\\\\.|\"(?!\"\"))*\"\"\"|\"(?:[^\"\\\\\\n]|\\\\.)*\")\\s*(?=(?:query|mutation|subscription|fragment)\\b|\\$\\w+\\s*:)");

    private final DocumentSource delegate;

    DescriptionStrippingDocumentSource(Resource location) {
        this.delegate = new ResourceDocumentSource(List.of(location), List.of(".graphql"));
    }

    @Override
    public Mono<String> getDocument(String name) {
        return delegate.getDocument(name).map(DescriptionStrippingDocumentSource::strip);
    }

    static String strip(String document) {
        return DESCRIPTION.matcher(document).replaceAll("");
    }
}
