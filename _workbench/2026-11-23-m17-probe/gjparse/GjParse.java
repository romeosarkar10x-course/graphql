import graphql.language.*;
import graphql.parser.*;
import java.util.*;

/** How graphql-java 26.1 parses edge cases (accept or reject, and what it makes of them). */
public class GjParse {
    public static void main(String[] a) {
        String[] docs = {
            "\"desc\" query Q { a }",
            "\"\"\"frag\"\"\" fragment F on T { a } { ...F }",
            "query ($x: Int, \"var desc\" $y: Int) { a }",
            "{ a { } }",
            "{ a(s: \"tab\there\") }",
            "{ a(s: \"ctrl\u0001x\") }",
            "{ a(s: \"\\u{1F600}\") }",
            "{ a(s: \"\\uD83D\\uDE00\") }",
            "{ a(s: \"\\uD83D\") }",
            "{ a(s: \"\\u{110000}\") }",
            "{ a(s: \"😀😀\") b }",
            "{ a(n: 0x1) }",
            "{ a(n: 1.) }",
            "{ a(n: 00) }",
            "{ a(n: 1e) }",
            "{ a(n: -0) }",
            "{ a(n: 1.5e+3) }",
            "{ a(s: \"\"\"\n    hello\n      world\n  \"\"\") }",
            "{ a(s: \"\"\" \\\"\"\" \"\"\") }",
            "\uFEFF{ a }",
            "{ a(s: \"\\x\") }",
            "{ a.b }",
            "{ a(n: 1..2) }",
            "{ a(n: true1) }",
            "query { a } extend type Q { b: Int }",
            "type Q { a: Int }",
            "{ a(n: \"\u2028\") }",
            "{ a(s: \"\\u00e9\\U00e9\") }",
        };
        for (String d : docs) {
            String shown = d.replace("\n", "\\n").replace("\t", "\\t").replace("\u0001", "\\u0001").replace("\uFEFF", "<BOM>").replace("\u2028","<U+2028>");
            try {
                Document doc = new Parser().parseDocument(d);
                StringBuilder sb = new StringBuilder();
                new NodeVisitorStub();
                for (Definition<?> def : doc.getDefinitions()) sb.append(def.getClass().getSimpleName()).append('@').append(loc(def)).append(' ');
                for (var n : collect(doc)) sb.append(n).append(' ');
                System.out.println("OK   " + shown + "  ->  " + sb.toString().trim());
            } catch (Exception e) {
                System.out.println("FAIL " + shown + "  ->  " + e.getClass().getSimpleName() + ": " + e.getMessage().replace("\n"," "));
            }
        }
    }
    static String loc(Node<?> n) { var l = n.getSourceLocation(); return l == null ? "?" : l.getLine() + ":" + l.getColumn(); }
    static List<String> collect(Node<?> root) {
        List<String> out = new ArrayList<>();
        new NodeTraverser().depthFirst(new NodeVisitorStub() {
            @Override public graphql.util.TraversalControl visitField(Field f, graphql.util.TraverserContext<Node> c) { out.add("field " + f.getName() + "@" + loc(f) + (f.getSelectionSet() == null ? "" : "{" + f.getSelectionSet().getSelections().size() + "}")); return graphql.util.TraversalControl.CONTINUE; }
            @Override public graphql.util.TraversalControl visitStringValue(StringValue v, graphql.util.TraverserContext<Node> c) { out.add("string " + v.getValue().codePoints().mapToObj(cp -> cp < 0x20 || cp > 0x7e ? String.format("U+%04X", cp) : Character.toString(cp)).reduce("", String::concat)); return graphql.util.TraversalControl.CONTINUE; }
            @Override public graphql.util.TraversalControl visitOperationDefinition(OperationDefinition o, graphql.util.TraverserContext<Node> c) { out.add("op"); return graphql.util.TraversalControl.CONTINUE; }
            @Override public graphql.util.TraversalControl visitVariableDefinition(VariableDefinition o, graphql.util.TraverserContext<Node> c) { out.add("var " + o.getName() + "@" + loc(o)); return graphql.util.TraversalControl.CONTINUE; }
            @Override public graphql.util.TraversalControl visitArgument(Argument o, graphql.util.TraverserContext<Node> c) { out.add("arg@" + loc(o)); return graphql.util.TraversalControl.CONTINUE; }
        }, root);
        return out;
    }
}
