package dev.shelfie.boot;

import java.util.concurrent.TimeoutException;

/** Provided: Shelfie's domain exceptions. Do not edit. */
public final class Problems {
    private Problems() {}
    public static class NotFound extends RuntimeException { public NotFound(String m) { super(m); } }
    public static class Forbidden extends RuntimeException { public Forbidden(String m) { super(m); } }
    public static class Invalid extends RuntimeException {
        public final String field;
        public Invalid(String field, String m) { super(m); this.field = field; }
    }
    public static class Conflict extends RuntimeException {
        public final int currentVersion;
        public Conflict(int currentVersion, String m) { super(m); this.currentVersion = currentVersion; }
    }

    public static String trouble(String kind) throws Exception {
        switch (kind) {
            case "NOT_FOUND" -> throw new NotFound("No such club zz");
            case "FORBIDDEN" -> throw new Forbidden("Not allowed to read this club's notes");
            case "INVALID" -> throw new Invalid("rating", "Rating must be 1 to 5, got 9");
            case "CONFLICT" -> throw new Conflict(7, "The club was changed by someone else");
            case "TIMEOUT" -> throw new TimeoutException("ratings-service did not answer in 2000 ms");
            default -> throw new IllegalStateException("db=jdbc:postgresql://10.0.3.7/shelfie pool exhausted");
        }
    }
}
