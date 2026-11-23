package dev.toolshed.common;

/** Signed in, but not allowed: classification FORBIDDEN. */
public class Forbidden extends RuntimeException {
    public Forbidden(String message) {
        super(message);
    }
}
