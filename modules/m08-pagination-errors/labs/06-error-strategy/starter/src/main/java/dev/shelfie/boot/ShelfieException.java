package dev.shelfie.boot;

/** A domain failure the client is meant to see: its message is safe to show and its code is stable. */
public class ShelfieException extends RuntimeException {
    private final ShelfieError code;

    public ShelfieException(ShelfieError code, String message) {
        super(message);
        this.code = code;
    }

    public ShelfieError code() { return code; }
}
