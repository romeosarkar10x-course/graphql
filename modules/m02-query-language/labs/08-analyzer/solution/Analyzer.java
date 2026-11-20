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
 * Lab 2.8 reference: a GraphQL document is data. Parse it, expand its fragments, walk the tree.
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
        // The transformer also walks into the selections it just inserted, so nested spreads expand in
        // the same pass, and a cycle would expand forever. Validation would reject a cycle; check first.
        for (String name : fragments.keySet()) findCycle(name, fragments, new java.util.ArrayDeque<>());
        return (OperationDefinition) new AstTransformer().transform(op, new NodeVisitorStub() {
            @Override
            public TraversalControl visitFragmentSpread(FragmentSpread spread, TraverserContext<Node> context) {
                FragmentDefinition def = fragments.get(spread.getName());
                if (def == null) fail("unknown fragment " + spread.getName());
                InlineFragment inline = InlineFragment.newInlineFragment()
                        .typeCondition(def.getTypeCondition())
                        .directives(spread.getDirectives())
                        // deepCopy: the traverser visits each node instance once, so a fragment spread
                        // twice must become two separate subtrees, or its fields are counted once.
                        .selectionSet(def.getSelectionSet().deepCopy())
                        .build();
                return TreeTransformerUtil.changeNode(context, inline);
            }
        });
    }

    /** Depth-first search over "fragment A spreads fragment B"; a name already on the path is a cycle. */
    static void findCycle(String name, Map<String, FragmentDefinition> fragments, java.util.Deque<String> path) {
        if (path.contains(name)) fail("fragment cycle: " + String.join(" -> ", path) + " -> " + name);
        FragmentDefinition def = fragments.get(name);
        if (def == null) return; // reported as unknown when expanded
        path.addLast(name);
        for (String next : spreadsIn(def)) findCycle(next, fragments, path);
        path.removeLast();
    }

    static java.util.Set<String> spreadsIn(Node<?> root) {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        new NodeTraverser().preOrder(new NodeVisitorStub() {
            @Override
            public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
                names.add(node.getName());
                return TraversalControl.CONTINUE;
            }
        }, root);
        return names;
    }

    static final class Metrics {
        int depth, fields, aliases;
    }

    static Metrics measure(OperationDefinition op) {
        Metrics m = new Metrics();
        new NodeTraverser().preOrder(new NodeVisitorStub() {
            @Override
            public TraversalControl visitField(Field field, TraverserContext<Node> context) {
                m.fields++;
                if (field.getAlias() != null) m.aliases++;
                // Depth = this field plus every field above it on the path to the root.
                int depth = 1 + (int) context.getParentNodes().stream().filter(n -> n instanceof Field).count();
                m.depth = Math.max(m.depth, depth);
                return TraversalControl.CONTINUE;
            }
        }, op);
        return m;
    }

    static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
