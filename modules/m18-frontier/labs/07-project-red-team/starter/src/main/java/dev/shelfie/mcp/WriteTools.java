package dev.shelfie.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Writes. Calling join_club or create_review changes nothing: it files an
 * approval request for the signed-in caller and says so. A human approves it out of band; then confirm_action runs
 * the operation, once, as that caller.
 */
@Component
@ConditionalOnProperty(name = "shelfie.mcp.writes", havingValue = "true", matchIfMissing = true)
public class WriteTools {
    private final Approvals approvals;
    private final Shelfie shelfie;
    private final Audit audit;

    WriteTools(Approvals approvals, Shelfie shelfie, Audit audit) {
        this.approvals = approvals;
        this.shelfie = shelfie;
        this.audit = audit;
    }

    @Tool(name = "join_club", description = """
            Ask to make the signed-in reader a member of a club. Nothing changes yet: this returns an approvalId that
            a human must approve; then call confirm_action with it. Fails when no reader is signed in.""")
    public Map<String, Object> joinClub(@ToolParam(description = "Club id, e.g. c2") String clubId) {
        return request("join_club", "JoinClub", Map.of("clubId", clubId), "join club " + clubId);
    }

    @Tool(name = "create_review", description = """
            Ask to publish a review as the signed-in reader: rating 1 to 5, body of 10 to 2000 characters. Nothing
            is published yet: this returns an approvalId that a human must approve; then call confirm_action.""")
    public Map<String, Object> createReview(
            @ToolParam(description = "Book id, e.g. b2") String bookId,
            @ToolParam(description = "1 to 5") int rating,
            @ToolParam(description = "The review, 10 to 2000 characters") String body) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("bookId", bookId);
        vars.put("rating", rating);
        vars.put("body", body);
        return request("create_review", "CreateReview", vars, "review " + bookId + " with " + rating + " stars: " + body);
    }

    @Tool(name = "confirm_action", description = """
            Run a write a human has approved, once: pass the approvalId that join_club or create_review returned.
            Refused while the approval is pending, after it was used or expired, or for another reader's approval.""")
    public Map<String, Object> confirmAction(@ToolParam(description = "The approvalId, apr_...") String approvalId) {
        return audit.record("confirm_action", Map.of("approvalId", approvalId), () -> {
            String user = Caller.user().orElseThrow(() -> new PolicyException("denied: no reader is signed in (X-Shelfie-User)"));
            Approvals.Request r = approvals.take(approvalId, user);
            return shelfie.operation(r.operation, r.variables);
        });
    }

    private Map<String, Object> request(String tool, String operation, Map<String, Object> vars, String summary) {
        String user = Caller.user().orElse(null);
        if (user == null) {
            audit.denied(tool, "no reader is signed in");
            throw new PolicyException("denied: writes need a signed-in reader (the client sends X-Shelfie-User)");
        }
        Approvals.Request r = approvals.file(user, operation, vars, summary);
        audit.pending(tool, r.id + " " + summary);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "APPROVAL_REQUIRED");
        out.put("approvalId", r.id);
        out.put("summary", "As " + user + ": " + summary);
        out.put("expiresAt", r.expires.toString());
        out.put("next", "Ask the human to approve it, then call confirm_action with the approvalId.");
        return out;
    }
}
