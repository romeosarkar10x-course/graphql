package dev.shelfie.mcp;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Writes wait for a human. A write tool files a request bound to the caller; the human approves it out of band
 * (POST /approvals/{id} with the approver token, a route the agent has no tool for); then the same caller's agent
 * confirms it, once, before it expires.
 */
@Component
class Approvals {
    enum State { PENDING, APPROVED, DENIED, DONE }

    static final class Request {
        final String id, user, operation, summary;
        final Map<String, Object> variables;
        final Instant expires;
        volatile State state = State.PENDING;

        Request(String id, String user, String operation, Map<String, Object> variables, String summary, Instant expires) {
            this.id = id; this.user = user; this.operation = operation; this.variables = variables;
            this.summary = summary; this.expires = expires;
        }

        Map<String, Object> view() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("approvalId", id); m.put("state", state.name()); m.put("user", user); m.put("operation", operation);
            m.put("variables", variables); m.put("summary", summary); m.put("expiresAt", expires.toString());
            return m;
        }
    }

    private final Map<String, Request> requests = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Policy policy;

    Approvals(Policy policy) {
        this.policy = policy;
    }

    Request file(String user, String operation, Map<String, Object> variables, String summary) {
        byte[] b = new byte[16];
        random.nextBytes(b);
        String id = "apr_" + HexFormat.of().formatHex(b);
        Request r = new Request(id, user, operation, Map.copyOf(variables), summary, Instant.now().plus(policy.approvalTtl()));
        requests.put(id, r);
        return r;
    }

    /** The human's decision. False when there is no such pending request. */
    boolean decide(String id, boolean approve) {
        Request r = requests.get(id);
        if (r == null || r.state != State.PENDING || Instant.now().isAfter(r.expires)) return false;
        r.state = approve ? State.APPROVED : State.DENIED;
        return true;
    }

    /** The agent's confirmation: the request, approved. */
    synchronized Request take(String id, String user) {
        Request r = requests.get(id);
        if (r == null) throw new PolicyException("denied: no approval " + id);
        switch (r.state) {
            case PENDING -> throw new PolicyException("denied: approval " + id + " is still waiting for a human");
            case DENIED -> throw new PolicyException("denied: a human refused " + id);
            case DONE -> throw new PolicyException("denied: approval " + id + " was already used");
            case APPROVED -> { }
        }
        return r;
    }

    List<Map<String, Object>> all() {
        return requests.values().stream().sorted(Comparator.comparing(r -> r.expires)).map(Request::view).toList();
    }
}
