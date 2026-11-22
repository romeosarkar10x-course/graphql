package dev.shelfie.mcp;

/** A call the policy refuses. The message goes back to the agent as a tool error; nothing reaches Shelfie. */
class PolicyException extends RuntimeException {
    PolicyException(String message) {
        super(message);
    }
}
