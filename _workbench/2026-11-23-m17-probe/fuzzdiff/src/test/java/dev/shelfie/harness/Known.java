package dev.shelfie.harness;

/**
 * Where graphql-java 26.1 and the September 2025 spec part ways (probed, _workbench/2026-11-23-m17-probe). A
 * disagreement on such a document is not counted against you, provided you follow the spec.
 */
public final class Known {
    private Known() {}

    /** Why the lexers may disagree on this document, or null. */
    public static String lexer(String doc) {
        if (doc.indexOf(' ') >= 0 || doc.indexOf(' ') >= 0) return "graphql-java treats U+2028 and U+2029 as line terminators";
        if (doc.replace("\r\n", "").indexOf('\r') >= 0) return "graphql-java does not start a new line at a lone CR, so its line numbers fall behind";
        return null;
    }

    private static final java.util.regex.Pattern EMPTY_BRACES =
            java.util.regex.Pattern.compile("\\b(type|interface|enum|input)[\\s,]+[_A-Za-z][_0-9A-Za-z]*[^{}\"]*\\{[\\s,]*\\}");

    /**
     * Why the parsers may disagree on this document, or null. mine is your Document and theirs graphql-java's as your
     * records, each null when that parser rejected the document.
     */
    public static String parser(String doc, Object mine, Object theirs) {
        boolean theyAccepted = theirs != null;
        String lexical = lexer(doc);
        if (lexical != null) return lexical;
        if (blockStrings(doc)) return "graphql-java computes block string values differently (see blockString)";
        if (mine instanceof dev.shelfie.mini.Ast.Document d && !theyAccepted && d.definitions().stream().anyMatch(Known::describedExecutable))
            return "descriptions on operations, fragments and variables are new in September 2025; graphql-java 26.1 rejects them";
        if (mine == null && theyAccepted && EMPTY_BRACES.matcher(doc.replaceAll("#[^\\n\\r]*", "")).find())
            return "graphql-java accepts {} with no fields, values or input fields; the spec needs at least one";
        if (mine == null && theirs instanceof dev.shelfie.mini.Ast.Document d && fieldlessThenShorthand(d))
            return "graphql-java reads `type T { ... }` as a fieldless type and a query when the braces are not fields; the spec's [lookahead != {] forbids it";
        return null;
    }

    private static boolean fieldlessThenShorthand(dev.shelfie.mini.Ast.Document d) {
        var defs = d.definitions();
        for (int i = 0; i + 1 < defs.size(); i++) {
            boolean fieldless = switch (defs.get(i)) {
                case dev.shelfie.mini.Ast.ObjectDef o -> o.fields().isEmpty();
                case dev.shelfie.mini.Ast.InterfaceDef o -> o.fields().isEmpty();
                case dev.shelfie.mini.Ast.InputDef o -> o.fields().isEmpty();
                case dev.shelfie.mini.Ast.EnumDef o -> o.values().isEmpty();
                default -> false;
            };
            if (fieldless && defs.get(i + 1) instanceof dev.shelfie.mini.Ast.Operation op && op.name() == null && op.variables().isEmpty()) return true;
        }
        return false;
    }

    private static boolean describedExecutable(dev.shelfie.mini.Ast.Definition d) {
        return switch (d) {
            case dev.shelfie.mini.Ast.Operation o -> o.description() != null || o.variables().stream().anyMatch(v -> v.description() != null);
            case dev.shelfie.mini.Ast.Fragment f -> f.description() != null;
            default -> false;
        };
    }

    /** True when a block string's value would differ between the spec and graphql-java (see blockString). */
    public static boolean blockStrings(String doc) {
        try {
            for (String raw : Gj.blockStringTokens(doc))
                if (!blockString(raw).equals(graphql.parser.StringValueParsing.parseTripleQuotedString(raw))) return true;
        } catch (RuntimeException e) {
            return false;   // graphql-java cannot even lex it
        }
        return false;
    }

    /**
     * BlockStringValue(rawValue), spec 2.9.4, for the raw text of a block string token (quotes included). graphql-java's
     * own version differs twice: it splits lines on LF only, so CR stays in the value, and it removes the common
     * indentation only from lines longer than it, so a short whitespace-only line keeps its spaces.
     */
    public static String blockString(String token) {
        String raw = token.substring(3, token.length() - 3).replace("\\\"\"\"", "\"\"\"");
        java.util.List<String> lines = new java.util.ArrayList<>(java.util.List.of(raw.split("\r\n|\n|\r", -1)));
        Integer common = null;
        for (int i = 1; i < lines.size(); i++) {
            int indent = indent(lines.get(i));
            if (indent < lines.get(i).length() && (common == null || indent < common)) common = indent;
        }
        if (common != null)
            for (int i = 1; i < lines.size(); i++) lines.set(i, lines.get(i).substring(Math.min(common, lines.get(i).length())));
        while (!lines.isEmpty() && indent(lines.getFirst()) == lines.getFirst().length()) lines.removeFirst();
        while (!lines.isEmpty() && indent(lines.getLast()) == lines.getLast().length()) lines.removeLast();
        return String.join("\n", lines);
    }

    private static int indent(String line) {
        int n = 0;
        while (n < line.length() && (line.charAt(n) == ' ' || line.charAt(n) == '\t')) n++;
        return n;
    }
}
