package dev.toolshed.common;

/** Needs a signed-in member: classification UNAUTHORIZED. */
public class Unauthenticated extends RuntimeException {
    public Unauthenticated(String message) {
        super(message);
    }
}
