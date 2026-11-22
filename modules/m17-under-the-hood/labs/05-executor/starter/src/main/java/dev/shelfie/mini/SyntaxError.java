package dev.shelfie.mini;

/** A lexer or parser failure, at a line and column (both from 1). */
public class SyntaxError extends RuntimeException {
    public final int line, column;

    public SyntaxError(String message, int line, int column) {
        super("Syntax Error: " + message);
        this.line = line;
        this.column = column;
    }
}
