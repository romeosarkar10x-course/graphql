package dev.shelfie.harness;

import dev.shelfie.mini.Lexer;
import dev.shelfie.mini.SyntaxError;
import dev.shelfie.mini.Token;
import java.util.ArrayList;
import java.util.List;

/**
 * Lab 17.2's differential test: your lexer and graphql-java's on every corpus document. Both reject, or both
 * produce the same tokens (kind, value, line and column). Prints one line per disagreement and a summary line
 * "agree A of N; disagree D; known K".
 *
 *   TokenDiff <count> <seed> [--show N]
 */
public final class TokenDiff {
    public static void main(String[] a) throws Exception {
        List<String> docs = Corpus.documents(Integer.parseInt(a[0]), Long.parseLong(a[1]));
        int show = a.length > 3 ? Integer.parseInt(a[3]) : 10, agree = 0, disagree = 0, known = 0;
        for (int i = 0; i < docs.size(); i++) {
            String doc = docs.get(i);
            String mine, theirs;
            try { mine = String.join("\n", ours(doc)); } catch (SyntaxError e) { mine = "REJECT"; }
            catch (RuntimeException e) { mine = "CRASH " + e; }
            try { theirs = String.join("\n", Gj.tokens(doc)); } catch (RuntimeException e) { theirs = "REJECT"; }
            if (mine.equals(theirs)) { agree++; continue; }
            String why = Known.lexer(doc);
            if (why != null) { known++; continue; }
            disagree++;
            if (disagree <= show) System.out.println("DISAGREE doc " + i + ": " + firstDifference(mine, theirs));
        }
        System.out.println("agree " + agree + " of " + docs.size() + "; disagree " + disagree + "; known " + known);
    }

    static List<String> ours(String doc) {
        List<String> out = new ArrayList<>();
        for (Token t : Lexer.lex(doc)) {
            String s = switch (t) {
                case Token.Punct p -> "Punct " + p.text();
                case Token.Name n -> "Name " + n.value();
                case Token.IntValue v -> "Int " + v.text();
                case Token.FloatValue v -> "Float " + v.text();
                case Token.StringValue v -> "String " + v.value();
                case Token.End e -> null;
            };
            if (s != null) out.add(s + " @" + t.line() + ":" + t.column());
        }
        return out;
    }

    static String firstDifference(String a, String b) {
        String[] x = a.split("\n", -1), y = b.split("\n", -1);
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            String p = i < x.length ? x[i] : "<none>", q = i < y.length ? y[i] : "<none>";
            if (!p.equals(q)) return "token " + i + ": yours [" + esc(p) + "] graphql-java [" + esc(q) + "]";
        }
        return "?";
    }

    static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        s.codePoints().forEach(c -> sb.append(c < 0x20 || c > 0x7e ? String.format("\\u{%X}", c) : Character.toString(c)));
        return sb.length() > 160 ? sb.substring(0, 160) + "..." : sb.toString();
    }
}
