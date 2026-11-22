package dev.shelfie.mini;

/**
 * A lexical token (spec 2.1.6). Every variant records where it starts: line and column, both from 1, columns
 * counted in Unicode code points.
 */
public sealed interface Token {
    int line();
    int column();

    /** One of ! $ & ( ) ... : = @ [ ] { | } */
    record Punct(String text, int line, int column) implements Token {}

    record Name(String value, int line, int column) implements Token {}

    /** The source text, e.g. "-12". */
    record IntValue(String text, int line, int column) implements Token {}

    /** The source text, e.g. "1.5e3". */
    record FloatValue(String text, int line, int column) implements Token {}

    /** The string's value: escapes resolved, and for a block string the BlockStringValue() algorithm applied. */
    record StringValue(String value, boolean block, int line, int column) implements Token {}

    /** End of the document (the spec's <EOF>). */
    record End(int line, int column) implements Token {}
}
