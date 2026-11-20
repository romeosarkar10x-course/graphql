import graphql.language.*;
import graphql.parser.Parser;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TreeTransformerUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lab 2.8: a GraphQL document is data. Parse it, expand its fragments, walk the tree.
 *
 * Run: bin/course java Analyzer.java query.graphql            one JSON line per operation
 *      bin/course java Analyzer.java query.graphql --print    the operations with fragments expanded
 *
 * Metrics, counted after every fragment spread is replaced by the fragment's selections:
 *   depth    the longest chain of nested fields (a root field alone is depth 1)
 *   fields   every field node, __typename included, each time it appears
 *   aliases  every field node that has an alias
 */
public class Analyzer {

    public static void main(String[] args) throws Exception {
        Document doc;
        try {
            doc = Parser.parse(Files.readString(Path.of(args[0])));
        } catch (graphql.parser.InvalidSyntaxException e) {
            System.err.println("syntax error: " + e.getMessage());
            System.exit(1);
            return;
        }
        boolean print = args.length > 1 && args[1].equals("--print");
        Map<String, FragmentDefinition> fragments = doc.getDefinitionsOfType(FragmentDefinition.class).stream()
                .collect(Collectors.toMap(FragmentDefinition::getName, Function.identity()));

        for (OperationDefinition op : doc.getDefinitionsOfType(OperationDefinition.class)) {
            OperationDefinition expanded = expand(op, fragments);
            if (print) {
                System.out.println(AstPrinter.printAst(expanded));
                continue;
            }
            Metrics m = measure(expanded);
            String name = op.getName() == null ? null : "\"" + op.getName() + "\"";
            System.out.printf("{\"operation\":%s,\"type\":\"%s\",\"depth\":%d,\"fields\":%d,\"aliases\":%d}%n",
                    name, op.getOperation().name().toLowerCase(), m.depth, m.fields, m.aliases);
        }
    }

    /** Replace every spread with an inline fragment carrying the same type condition and directives. */
    static OperationDefinition expand(OperationDefinition op, Map<String, FragmentDefinition> fragments) {
        // TODO 1: report a fragment cycle (exit 1 via fail) before expanding anything.
        // TODO 2: use AstTransformer with a NodeVisitorStub whose visitFragmentSpread replaces the spread
        //         (TreeTransformerUtil.changeNode) with an InlineFragment built from the definition.
        //         An unknown fragment name is an error too.
        return op;
    }

    static final class Metrics {
        int depth, fields, aliases;
    }

    static Metrics measure(OperationDefinition op) {
        Metrics m = new Metrics();
        // TODO 3: walk the operation with NodeTraverser and a NodeVisitorStub; count fields and aliases,
        //         and track the deepest chain of nested fields.
        return m;
    }

    static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
