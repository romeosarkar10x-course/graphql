package dev.shelfie.mcp;

import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Component;

/** The quickest MCP server there is: one tool that runs whatever GraphQL the agent writes. */
@Component
public class ShelfieTools {
    private final HttpSyncGraphQlClient shelfie;

    ShelfieTools(HttpSyncGraphQlClient shelfie) {
        this.shelfie = shelfie;
    }

    @Tool(name = "run_graphql", description = "Run any GraphQL query or mutation against Shelfie and return the data.")
    public Map<String, Object> runGraphql(@ToolParam(description = "A GraphQL document") String query) {
        ClientGraphQlResponse response = shelfie.document(query).executeSync();
        if (!response.getErrors().isEmpty()) {
            throw new IllegalStateException(response.getErrors().getFirst().getMessage());
        }
        Map<String, Object> data = response.getData();
        return data == null ? Map.of() : data;
    }
}
