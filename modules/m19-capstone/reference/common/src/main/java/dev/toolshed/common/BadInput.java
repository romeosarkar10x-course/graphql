package dev.toolshed.common;

/** A client mistake in an argument: a GraphQL error with classification BAD_REQUEST and the argument's name. */
public class BadInput extends RuntimeException {
    private final String argument;

    public BadInput(String argument, String message) {
        super(message);
        this.argument = argument;
    }

    public String argument() {
        return argument;
    }
}
