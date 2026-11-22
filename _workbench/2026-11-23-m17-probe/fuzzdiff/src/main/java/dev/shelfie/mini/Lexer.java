package dev.shelfie.mini;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexical grammar (spec 2.1). Greedy: each token is the longest possible sequence of characters, and a number may
 * not be followed by a digit, a '.' or a name start (2.1.8, 2.1.9). Ignored tokens (white space, line terminators,
 * comments, commas and the byte order mark) are skipped.
 */
public final class Lexer {
    private final int[] src;   // code points, so columns and surrogate pairs come out right
    private int pos, line = 1, lineStart;

    private Lexer(String source) { src = source.codePoints().toArray(); }

    public static List<Token> lex(String source) {
        Lexer l = new Lexer(source);
        List<Token> tokens = new ArrayList<>();
        Token t;
        do { tokens.add(t = l.next()); } while (!(t instanceof Token.End));
        return tokens;
    }

    private int peek(int ahead) { return pos + ahead < src.length ? src[pos + ahead] : -1; }
    private int col() { return pos - lineStart + 1; }
    private SyntaxError error(String message) { return new SyntaxError(message, line, col()); }

    private void newline() {
        if (src[pos] == '\r' && peek(1) == '\n') pos++;
        pos++;
        line++;
        lineStart = pos;
    }

    private Token next() {
        while (pos < src.length) {
            int c = src[pos];
            if (c == '\n' || c == '\r') newline();
            else if (c == ' ' || c == '\t' || c == ',' || c == 0xFEFF) pos++;
            else if (c == '#') { while (pos < src.length && src[pos] != '\n' && src[pos] != '\r') pos++; }
            else break;
        }
        int ln = line, cl = col();
        if (pos >= src.length) return new Token.End(ln, cl);
        int c = src[pos];
        if ("!$&()[]{}:=@|".indexOf(c) >= 0) { pos++; return new Token.Punct(Character.toString(c), ln, cl); }
        if (c == '.') {
            if (peek(1) == '.' && peek(2) == '.') { pos += 3; return new Token.Punct("...", ln, cl); }
            throw error("Unexpected \".\"; did you mean \"...\"?");
        }
        if (nameStart(c)) {
            int start = pos;
            while (pos < src.length && (nameStart(src[pos]) || digit(src[pos]))) pos++;
            return new Token.Name(text(start, pos), ln, cl);
        }
        if (c == '-' || digit(c)) return number(ln, cl);
        if (c == '"') return peek(1) == '"' && peek(2) == '"' ? blockString(ln, cl) : string(ln, cl);
        throw error("Unexpected character " + describe(c) + ".");
    }

    private Token number(int ln, int cl) {
        int start = pos;
        boolean isFloat = false;
        if (src[pos] == '-') pos++;
        if (peek(0) == '0') {
            pos++;
            if (digit(peek(0))) throw error("Invalid number, unexpected digit after 0: " + describe(peek(0)) + ".");
        } else digits();
        if (peek(0) == '.') { isFloat = true; pos++; digits(); }
        if (peek(0) == 'e' || peek(0) == 'E') {
            isFloat = true;
            pos++;
            if (peek(0) == '+' || peek(0) == '-') pos++;
            digits();
        }
        if (peek(0) == '.' || nameStart(peek(0)))
            throw error("Invalid number, expected digit but got: " + describe(peek(0)) + ".");
        String text = text(start, pos);
        return isFloat ? new Token.FloatValue(text, ln, cl) : new Token.IntValue(text, ln, cl);
    }

    private void digits() {
        if (!digit(peek(0))) throw error("Invalid number, expected digit but got: " + describe(peek(0)) + ".");
        while (digit(peek(0))) pos++;
    }

    private Token string(int ln, int cl) {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            int c = peek(0);
            if (c == -1 || c == '\n' || c == '\r') throw error("Unterminated string.");
            if (c == '"') { pos++; return new Token.StringValue(sb.toString(), false, ln, cl); }
            if (c == '\\') { pos++; escape(sb); continue; }
            sb.appendCodePoint(c);
            pos++;
        }
    }

    private void escape(StringBuilder sb) {
        int c = peek(0);
        pos++;
        switch (c) {
            case '"' -> sb.append('"');
            case '\\' -> sb.append('\\');
            case '/' -> sb.append('/');
            case 'b' -> sb.append('\b');
            case 'f' -> sb.append('\f');
            case 'n' -> sb.append('\n');
            case 'r' -> sb.append('\r');
            case 't' -> sb.append('\t');
            case 'u' -> {
                int cp = unicode();
                if (Character.isHighSurrogate((char) cp) && cp <= 0xFFFF && peek(0) == '\\' && peek(1) == 'u') {
                    int save = pos;
                    pos += 2;
                    int low = unicode();
                    if (low <= 0xFFFF && Character.isLowSurrogate((char) low)) { sb.appendCodePoint(Character.toCodePoint((char) cp, (char) low)); return; }
                    pos = save;
                }
                if (cp >= 0xD800 && cp <= 0xDFFF || cp > 0x10FFFF) throw error("Invalid Unicode escape sequence.");
                sb.appendCodePoint(cp);
            }
            default -> { pos--; throw error("Invalid character escape sequence: \\" + (c == -1 ? "" : Character.toString(c)) + "."); }
        }
    }

    /** After "\\u": four hex digits, or {hex...} (variable width, RFC #849). */
    private int unicode() {
        int value = 0, n = 0;
        if (peek(0) == '{') {
            pos++;
            while (peek(0) != '}') {
                int h = Character.digit(peek(0), 16);
                if (h < 0 || peek(0) > 0x7F || value > 0x10FFFF) throw error("Invalid Unicode escape sequence.");
                value = value * 16 + h;
                pos++;
                n++;
            }
            pos++;
            if (n == 0) throw error("Invalid Unicode escape sequence.");
            return value;
        }
        for (; n < 4; n++, pos++) {
            int h = Character.digit(peek(0), 16);
            if (h < 0 || peek(0) > 0x7F) throw error("Invalid Unicode escape sequence.");
            value = value * 16 + h;
        }
        return value;
    }

    private Token blockString(int ln, int cl) {
        pos += 3;
        StringBuilder raw = new StringBuilder();
        while (true) {
            int c = peek(0);
            if (c == -1) throw error("Unterminated string.");
            if (c == '"' && peek(1) == '"' && peek(2) == '"') { pos += 3; break; }
            if (c == '\\' && peek(1) == '"' && peek(2) == '"' && peek(3) == '"') { raw.append("\"\"\""); pos += 4; continue; }
            if (c == '\n' || c == '\r') { raw.append(c == '\r' && peek(1) == '\n' ? "\r\n" : Character.toString(c)); newline(); continue; }
            raw.appendCodePoint(c);
            pos++;
        }
        return new Token.StringValue(blockStringValue(raw.toString()), true, ln, cl);
    }

    /** BlockStringValue(rawValue), spec 2.9.4: drop the common indentation and leading and trailing blank lines. */
    static String blockStringValue(String raw) {
        List<String> lines = new ArrayList<>(List.of(raw.split("\r\n|\n|\r", -1)));
        int common = Integer.MAX_VALUE;
        for (int i = 1; i < lines.size(); i++) {
            String l = lines.get(i);
            int indent = 0;
            while (indent < l.length() && (l.charAt(indent) == ' ' || l.charAt(indent) == '\t')) indent++;
            if (indent < l.length()) common = Math.min(common, indent);
        }
        if (common != Integer.MAX_VALUE)
            for (int i = 1; i < lines.size(); i++) lines.set(i, lines.get(i).substring(Math.min(common, lines.get(i).length())));
        while (!lines.isEmpty() && blankOfSpaces(lines.getFirst())) lines.removeFirst();
        while (!lines.isEmpty() && blankOfSpaces(lines.getLast())) lines.removeLast();
        return String.join("\n", lines);
    }

    private static boolean blankOfSpaces(String l) { return l.chars().allMatch(ch -> ch == ' ' || ch == '\t'); }

    private String text(int from, int to) { return new String(src, from, to - from); }
    private static boolean nameStart(int c) { return c == '_' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'; }
    private static boolean digit(int c) { return c >= '0' && c <= '9'; }

    private static String describe(int c) {
        if (c == -1) return "<EOF>";
        return c >= 0x20 && c < 0x7F ? "\"" + Character.toString(c) + "\"" : String.format("U+%04X", c);
    }
}
