import java.util.concurrent.TimeoutException;

/** Provided: Shelfie's domain exceptions, and a fetcher body that throws the one `kind` names. Do not edit. */
final class Problems {
    private Problems() {}

    static final class NotFoundException extends RuntimeException {
        NotFoundException(String what) { super("No such " + what); }
    }
    static final class ForbiddenException extends RuntimeException {
        ForbiddenException(String action) { super("Not allowed to " + action); }
    }
    static final class InvalidInputException extends RuntimeException {
        final String field;
        InvalidInputException(String field, String message) { super(message); this.field = field; }
    }

    static Object trouble(String kind) throws Exception {
        switch (kind) {
            case "NOT_FOUND" -> throw new NotFoundException("club zz");
            case "FORBIDDEN" -> throw new ForbiddenException("read the notes of a private club");
            case "INVALID" -> throw new InvalidInputException("rating", "Rating must be 1 to 5, got 9");
            case "TIMEOUT" -> throw new TimeoutException("ratings-service did not answer in 2000 ms");
            default -> {
                String token = null;   // a bug: the message names internals
                return token.length() + " db=jdbc:postgresql://10.0.3.7/shelfie";
            }
        }
    }
}
