package dev.shelfie.mcp;

import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Component;

/**
 * What a product team asked for on Friday: let the assistant explore freely and act for the user. Everything here
 * runs at once, as whoever the agent says.
 */
@Component
public class AgentTools {
    private final HttpSyncGraphQlClient shelfie;

    AgentTools(HttpSyncGraphQlClient shelfie) {
        this.shelfie = shelfie;
    }

    @Tool(name = "run_graphql", description = "Run any GraphQL query or mutation against Shelfie and return the data.")
    public Map<String, Object> runGraphql(@ToolParam(description = "A GraphQL document") String query) {
        return data(shelfie.document(query).executeSync());
    }

    @Tool(name = "join_club", description = "Make a reader a member of a club, right away.")
    public Map<String, Object> joinClub(@ToolParam(description = "Club id, e.g. c2") String clubId,
                                        @ToolParam(description = "The reader to act as, e.g. u1") String userId) {
        return data(shelfie.mutate().header("X-Shelfie-User", userId).build()
                .documentName("JoinClub").variable("clubId", clubId).executeSync());
    }

    @Tool(name = "create_review", description = "Publish a review as a reader, right away.")
    public Map<String, Object> createReview(@ToolParam(description = "Book id") String bookId,
                                            @ToolParam(description = "1 to 5") int rating,
                                            @ToolParam(description = "10 to 2000 characters") String body,
                                            @ToolParam(description = "The reader to act as, e.g. u1") String userId) {
        return data(shelfie.mutate().header("X-Shelfie-User", userId).build().documentName("CreateReview")
                .variable("bookId", bookId).variable("rating", rating).variable("body", body).executeSync());
    }

    private static Map<String, Object> data(ClientGraphQlResponse response) {
        if (!response.getErrors().isEmpty()) throw new IllegalStateException(response.getErrors().getFirst().getMessage());
        Map<String, Object> data = response.getData();
        return data == null ? Map.of() : data;
    }
}
