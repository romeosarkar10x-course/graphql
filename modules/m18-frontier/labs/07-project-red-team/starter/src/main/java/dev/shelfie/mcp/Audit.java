package dev.shelfie.mcp;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** Every tool call, allowed or not, with who asked and why it was refused. GET /audit (approver token) reads it. */
@Component
class Audit {
    record Entry(Instant at, String tool, String user, String outcome, String detail) {}

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    void allowed(String tool, String detail) { add(tool, "allowed", detail); }
    void denied(String tool, String reason) { add(tool, "denied", reason); }
    void pending(String tool, String detail) { add(tool, "approval-required", detail); }

    private void add(String tool, String outcome, String detail) {
        entries.add(new Entry(Instant.now(), tool, Caller.user().orElse("-"), outcome, detail));
    }

    List<Entry> entries() { return List.copyOf(entries); }

    /** Run a tool body; a PolicyException is recorded as denied and re-thrown as the tool's error. */
    <T> T record(String tool, Map<String, Object> args, java.util.function.Supplier<T> body) {
        try {
            T result = body.get();
            allowed(tool, args.toString());
            return result;
        } catch (PolicyException e) {
            denied(tool, e.getMessage());
            throw e;
        }
    }
}
