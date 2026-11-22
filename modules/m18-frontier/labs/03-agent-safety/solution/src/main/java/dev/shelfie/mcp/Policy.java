package dev.shelfie.mcp;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The agent policy, in application.yaml under shelfie.mcp:
 *   writes          false (default): no write tools at all; true: write tools that only ask for approval
 *   raw-queries     true: run_graphql, a guarded read-only escape hatch
 *   max-cost        Module 10's cost function; run_graphql refuses more
 *   max-depth       deepest field nesting run_graphql accepts
 *   approver-token  the human's secret for /approvals (environment APPROVER_TOKEN); never given to the agent
 *   approval-ttl    how long an approval request stays open
 */
@ConfigurationProperties("shelfie.mcp")
record Policy(boolean writes, boolean rawQueries, int maxCost, int maxDepth, String approverToken, Duration approvalTtl) {
    Policy {
        if (maxCost <= 0) maxCost = 200;
        if (maxDepth <= 0) maxDepth = 6;
        if (approvalTtl == null) approvalTtl = Duration.ofMinutes(5);
    }
}
