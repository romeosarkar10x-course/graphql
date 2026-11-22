package dev.shelfie.app.service;

/** Failures the services raise; graph/ErrorAdvice turns them into GraphQL errors. */
public final class Problems {
    private Problems() {}

    public static class NotFound extends RuntimeException {
        public NotFound(String message) { super(message); }
    }

    public static class Forbidden extends RuntimeException {
        public Forbidden(String message) { super(message); }
    }
}
