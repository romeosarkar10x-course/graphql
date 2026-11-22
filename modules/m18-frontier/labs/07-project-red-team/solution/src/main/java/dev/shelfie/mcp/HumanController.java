package dev.shelfie.mcp;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The human's side, outside MCP: list approval requests, approve or deny one, read the audit log. Every route needs
 * the approver token (header X-Approver-Token), which the agent never sees.
 */
@RestController
class HumanController {
    private final Approvals approvals;
    private final Audit audit;
    private final byte[] token;

    HumanController(Approvals approvals, Audit audit, Policy policy) {
        this.approvals = approvals;
        this.audit = audit;
        this.token = policy.approverToken() == null || policy.approverToken().isBlank() ? null
                : policy.approverToken().getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/approvals")
    ResponseEntity<?> list(@RequestHeader(value = "X-Approver-Token", required = false) String t) {
        return human(t) ? ResponseEntity.ok(approvals.all()) : forbidden();
    }

    @PostMapping("/approvals/{id}")
    ResponseEntity<?> approve(@PathVariable String id, @RequestHeader(value = "X-Approver-Token", required = false) String t) {
        return decide(id, true, t);
    }

    @PostMapping("/approvals/{id}/deny")
    ResponseEntity<?> deny(@PathVariable String id, @RequestHeader(value = "X-Approver-Token", required = false) String t) {
        return decide(id, false, t);
    }

    @GetMapping("/audit")
    ResponseEntity<?> audit(@RequestHeader(value = "X-Approver-Token", required = false) String t) {
        return human(t) ? ResponseEntity.ok(audit.entries()) : forbidden();
    }

    private ResponseEntity<?> decide(String id, boolean approve, String t) {
        if (!human(t)) return forbidden();
        return approvals.decide(id, approve) ? ResponseEntity.ok(Map.of("approvalId", id, "approved", approve))
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "no pending approval " + id));
    }

    /** Constant-time comparison; with no token configured nobody can approve anything. */
    private boolean human(String t) {
        return token != null && t != null && MessageDigest.isEqual(token, t.getBytes(StandardCharsets.UTF_8));
    }

    private static ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "approver token required"));
    }
}
