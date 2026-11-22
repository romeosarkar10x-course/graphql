package dev.shelfie.mini;

import dev.shelfie.mini.Ast.Document;

/**
 * Lab 17.3: a recursive-descent parser for the whole grammar (spec appendix C.4): executable definitions and type
 * system definitions and extensions, into the records of Ast. One method per production. Give each node that has a
 * Loc the line and column of its first token. Throw SyntaxError for anything the grammar does not allow.
 */
public final class Parser {
    private Parser() {}

    public static Document parse(String source) {
        throw new UnsupportedOperationException("TODO lab 17.3: the parser");
    }
}
