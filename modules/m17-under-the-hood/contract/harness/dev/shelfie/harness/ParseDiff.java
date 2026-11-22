package dev.shelfie.harness;

import dev.shelfie.mini.Ast;
import dev.shelfie.mini.Parser;
import dev.shelfie.mini.SyntaxError;
import java.util.List;

/**
 * Lab 17.3's differential test: your parser and graphql-java's on every corpus document. Both reject, or both
 * produce the same tree: graphql-java's AST is converted to your records (Gj.toAst) and compared with equals(),
 * locations included. Prints disagreements and "agree A of N; disagree D; known K".
 *
 *   ParseDiff <count> <seed> [--show N]
 */
public final class ParseDiff {
    public static void main(String[] a) throws Exception {
        List<String> docs = Corpus.documents(Integer.parseInt(a[0]), Long.parseLong(a[1]));
        int show = a.length > 3 ? Integer.parseInt(a[3]) : 10, agree = 0, disagree = 0, known = 0;
        for (int i = 0; i < docs.size(); i++) {
            String doc = docs.get(i);
            Object mine, theirs;
            try { mine = Parser.parse(doc); } catch (SyntaxError e) { mine = "REJECT"; } catch (RuntimeException e) { mine = "CRASH " + e; }
            try { theirs = Gj.toAst(Gj.parse(doc)); } catch (graphql.parser.InvalidSyntaxException e) { theirs = "REJECT"; }
            if (mine.equals(theirs) || normal(mine).equals(normal(theirs))) { agree++; continue; }
            if (Known.parser(doc, mine instanceof String ? null : mine, theirs instanceof String ? null : theirs) != null) { known++; continue; }
            disagree++;
            if (disagree <= show) System.out.println("DISAGREE doc " + i + ": " + describe(mine, theirs));
        }
        System.out.println("agree " + agree + " of " + docs.size() + "; disagree " + disagree + "; known " + known);
    }

    /**
     * graphql-java keeps numbers as BigInteger and BigDecimal, not as written: 1e10 comes back as 1E+10, -0 as 0.
     * Both trees are compared with every number in that form.
     */
    static String normal(Object ast) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(IntV|FloatV)\\[text=([^\\]]+)]").matcher(String.valueOf(ast));
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String n = m.group(1).equals("IntV") ? new java.math.BigInteger(m.group(2)).toString() : new java.math.BigDecimal(m.group(2)).toString();
            m.appendReplacement(sb, m.group(1) + "[text=" + n + "]");
        }
        return m.appendTail(sb).toString();
    }

    static String describe(Object mine, Object theirs) {
        if (mine instanceof String || theirs instanceof String) return "yours " + head(mine) + " / graphql-java " + head(theirs);
        String x = mine.toString(), y = theirs.toString();
        int i = 0;
        while (i < Math.min(x.length(), y.length()) && x.charAt(i) == y.charAt(i)) i++;
        int from = Math.max(0, i - 60);
        return "first difference at char " + i + ":\n    yours        ..." + x.substring(from, Math.min(x.length(), i + 80))
                + "\n    graphql-java ..." + y.substring(from, Math.min(y.length(), i + 80));
    }

    static String head(Object o) { String s = String.valueOf(o); return s.length() > 100 ? s.substring(0, 100) + "..." : s; }
}
