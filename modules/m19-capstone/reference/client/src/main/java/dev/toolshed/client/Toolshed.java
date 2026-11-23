package dev.toolshed.client;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.graphql.client.FieldAccessException;
import org.springframework.graphql.client.GraphQlTransportException;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

/**
 * A typed client for Toolshed's operations (operations/*.graphql), on Spring's HttpSyncGraphQlClient.
 * Results are records; a typed failure (ToolUnavailable, ...) is a value, a GraphQL error an exception.
 */
public class Toolshed {
    public record Specs(String __typename, String summary) {}
    public record Named(String name) {}
    public record ToolCard(String id, String name, String condition, Boolean available, Specs specs, Named category, Named branch) {}
    public record Edge<T>(String cursor, T node) {}
    public record PageInfo(boolean hasNextPage, String endCursor) {}
    public record ToolPage(int totalCount, List<Edge<ToolCard>> edges, PageInfo pageInfo) {}
    public record LoanTool(String id, String name, Named branch) {}
    public record Loan(String id, String status, OffsetDateTime borrowedAt, OffsetDateTime dueAt, OffsetDateTime returnedAt, LoanTool tool) {}
    public record Loans(List<Edge<Loan>> edges, PageInfo pageInfo) {}
    public record Me(String id, String displayName, String email, Loans loans) {}
    /** BorrowTool's union, flattened: __typename says which member it is. */
    public record BorrowResult(String __typename, Map<String, Object> loan, String message, OffsetDateTime expectedBack, Integer limit, String field) {}

    private final HttpSyncGraphQlClient client;

    public Toolshed(String url, String token) {
        RestClient.Builder rest = RestClient.builder().baseUrl(url);
        if (token != null && !token.isBlank()) rest.defaultHeader("Authorization", "Bearer " + token);
        this.client = HttpSyncGraphQlClient.builder(rest.build()).interceptor(new PersistedDocuments()).build();
    }

    public ToolPage tools(int first, String after, Map<String, Object> filter) {
        return client.documentName("ToolPage").variable("first", first).variable("after", after).variable("filter", filter)
                .retrieveSync("tools").toEntity(ToolPage.class);
    }

    public Me me() {
        return client.documentName("MyLoans").retrieveSync("me").toEntity(Me.class);
    }

    /**
     * Borrows with a fresh idempotency key and retries once, with the same key, if the network fails: the
     * server answers the retry with the first result, so a lost response never borrows twice.
     */
    public BorrowResult borrow(String toolId, int days) {
        Map<String, Object> input = Map.of("toolId", toolId, "days", days, "idempotencyKey", UUID.randomUUID().toString());
        try {
            return borrowOnce(input);
        } catch (GraphQlTransportException e) {
            return borrowOnce(input);
        }
    }

    private BorrowResult borrowOnce(Map<String, Object> input) {
        return client.documentName("BorrowTool").variable("input", input).retrieveSync("borrowTool").toEntity(BorrowResult.class);
    }

    public Map<String, Object> giveBack(String loanId) {
        return client.documentName("ReturnTool").variable("loanId", loanId).retrieveSync("returnTool").toEntity(Map.class);
    }

    /** Raw access for the CLI's other commands. */
    public Object run(String operation, Map<String, Object> variables, String path) {
        try {
            return client.documentName(operation).variables(variables).retrieveSync(path).toEntity(Object.class);
        } catch (FieldAccessException e) {
            throw new IllegalStateException(e.getResponse().getErrors().toString());
        }
    }
}
