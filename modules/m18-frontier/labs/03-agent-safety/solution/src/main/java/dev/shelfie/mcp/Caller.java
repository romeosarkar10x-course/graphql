package dev.shelfie.mcp;

import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Who the agent acts for: the X-Shelfie-User header on the MCP HTTP request, set by whoever configured the client
 * (the human), never a tool argument the model could choose. Sync tools run on the servlet thread, so the request
 * is at hand.
 */
final class Caller {
    static final String HEADER = "X-Shelfie-User";
    private static final Pattern USER_ID = Pattern.compile("u[0-9]{1,6}");

    static Optional<String> user() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) return Optional.empty();
        String user = attrs.getRequest().getHeader(HEADER);
        return user != null && USER_ID.matcher(user).matches() ? Optional.of(user) : Optional.empty();
    }

    private Caller() {}
}
