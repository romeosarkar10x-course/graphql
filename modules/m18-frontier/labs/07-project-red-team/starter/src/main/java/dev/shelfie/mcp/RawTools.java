package dev.shelfie.mcp;

import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** The escape hatch, kept for questions no operation covers, behind QueryGuard. shelfie.mcp.raw-queries=false drops it. */
@Component
@ConditionalOnProperty(name = "shelfie.mcp.raw-queries", havingValue = "true", matchIfMissing = true)
public class RawTools {
    private final QueryGuard guard;
    private final Shelfie shelfie;
    private final Audit audit;

    RawTools(QueryGuard guard, Shelfie shelfie, Audit audit) {
        this.guard = guard;
        this.shelfie = shelfie;
        this.audit = audit;
    }

    @Tool(name = "run_graphql", description = """
            Run one read-only GraphQL query against Shelfie when no other tool fits. Write the values into the query
            (no variables). Refused: mutations and subscriptions, more than one operation, introspection, nesting
            deeper than 6, and a cost over 200 (a field costs 1 plus its children, times first or last).""")
    public Map<String, Object> runGraphql(@ToolParam(description = "One GraphQL query") String query) {
        return audit.record("run_graphql", Map.of("query", query), () -> shelfie.document(guard.check(query)));
    }
}
