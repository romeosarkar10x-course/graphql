import graphql.language.*;
import graphql.parser.*;
import java.nio.file.*;

/** Line and column graphql-java gives each field of a document file, with default options and with no limits. */
public class Lines {
    public static void main(String[] a) throws Exception {
        String src = Files.readString(Path.of(a[0]));
        ParserOptions big = ParserOptions.newParserOptions().maxTokens(Integer.MAX_VALUE).maxWhitespaceTokens(Integer.MAX_VALUE)
                .maxCharacters(Integer.MAX_VALUE).maxRuleDepth(Integer.MAX_VALUE).build();
        for (ParserOptions o : new ParserOptions[] {ParserOptions.getDefaultParserOptions(), big}) {
            Document d = new Parser().parseDocument(ParserEnvironment.newParserEnvironment().document(src).parserOptions(o).build());
            StringBuilder sb = new StringBuilder();
            new NodeTraverser().depthFirst(new NodeVisitorStub() {
                @Override public graphql.util.TraversalControl visitField(Field f, graphql.util.TraverserContext<Node> c) {
                    sb.append(f.getName()).append('@').append(f.getSourceLocation().getLine()).append(':').append(f.getSourceLocation().getColumn()).append(' ');
                    return graphql.util.TraversalControl.CONTINUE; }
            }, d);
            System.out.println((o == big ? "no limits: " : "defaults:  ") + sb);
        }
    }
}
