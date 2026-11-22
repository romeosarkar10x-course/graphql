package dev.shelfie.mcp;

import java.util.List;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

/**
 * Shelfie for AI agents, with a safety policy (Policy): curated read tools, a guarded raw query tool, and writes
 * that a human approves out of band. The caller is the X-Shelfie-User header of the MCP request.
 */
@SpringBootApplication
@EnableConfigurationProperties(Policy.class)
public class ShelfieMcpApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShelfieMcpApplication.class, args);
    }

    /** Persisted operations: documentName("FindBooks") loads graphql-documents/FindBooks.graphql, descriptions stripped. */
    @Bean
    HttpSyncGraphQlClient shelfieClient(@Value("${shelfie.graphql-url}") String url) {
        return HttpSyncGraphQlClient.builder(RestClient.create(url))
                .documentSource(new DescriptionStrippingDocumentSource(new ClassPathResource("graphql-documents/")))
                .build();
    }

    /** The tools the policy allows: always the reads; run_graphql and the writes only when configured. */
    @Bean
    ToolCallbackProvider toolCallbacks(ShelfieTools reads, ObjectProvider<RawTools> raw, ObjectProvider<WriteTools> writes) {
        List<Object> tools = new java.util.ArrayList<>(List.of(reads));
        raw.ifAvailable(tools::add);
        writes.ifAvailable(tools::add);
        return MethodToolCallbackProvider.builder().toolObjects(tools.toArray()).build();
    }
}
