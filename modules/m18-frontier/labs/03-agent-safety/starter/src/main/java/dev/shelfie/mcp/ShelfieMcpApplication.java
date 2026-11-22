package dev.shelfie.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

/** Shelfie for AI agents: an MCP server whose tools are Shelfie's described operations, nothing else. */
@SpringBootApplication
public class ShelfieMcpApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShelfieMcpApplication.class, args);
    }

    /** Persisted operations: documentName("FindBooks") loads graphql-documents/FindBooks.graphql, descriptions stripped. */
    @Bean
    HttpSyncGraphQlClient shelfie(@Value("${shelfie.graphql-url}") String url) {
        return HttpSyncGraphQlClient.builder(RestClient.create(url))
                .documentSource(new DescriptionStrippingDocumentSource(new ClassPathResource("graphql-documents/")))
                .build();
    }

    /** Every @Tool method of ShelfieTools and AgentTools becomes an MCP tool; its parameters become the tool's JSON input schema. */
    @Bean
    ToolCallbackProvider toolCallbacks(ShelfieTools tools, AgentTools agentTools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools, agentTools).build();
    }
}
