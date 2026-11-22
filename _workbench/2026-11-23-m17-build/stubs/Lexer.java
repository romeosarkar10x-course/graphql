package dev.shelfie.mini;

import java.util.List;

/**
 * Lab 17.2: the lexical grammar (spec 2.1 and 2.10, appendix C.1 to C.3). Turn source text into tokens, ending with
 * Token.End. Skip what the spec ignores (2.1.6). Lex greedily: a number may not be followed by a digit, a '.' or a
 * name start. Resolve string escapes, including \\u{...} (RFC #849), and apply BlockStringValue() (2.10.4) to block
 * strings. Throw SyntaxError, with the line and column where the problem is, for anything the grammar does not
 * allow. Lines and columns start at 1; count columns in code points, not chars.
 */
public final class Lexer {
    private Lexer() {}

    public static List<Token> lex(String source) {
        throw new UnsupportedOperationException("TODO lab 17.2: the lexer");
    }
}
