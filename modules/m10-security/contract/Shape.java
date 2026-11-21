// The shape of a GraphQL document, as a demand-control rule would see it (labs 10.3, 10.4).
//   bin/course java /course/modules/m10-security/contract/Shape.java <file.graphql>
// Prints: depth=<n> fields=<n> aliases=<n> maxSameField=<n> fragments=<n> cycle=<true|false>
// depth and fields count through fragment spreads and inline fragments; maxSameField is the most times
// one field name appears in one selection set under different aliases.
import graphql.language.*;
import graphql.parser.Parser;
import graphql.parser.ParserOptions;
import java.nio.file.*;
import java.util.*;

public class Shape {
  static Map<String, FragmentDefinition> fragments = new HashMap<>();
  static int fields, aliases, maxSame;
  static boolean cycle;

  public static void main(String[] a) throws Exception {
    // No parser limits here: we want to measure the attack, not refuse it.
    var opts = ParserOptions.newParserOptions().maxTokens(Integer.MAX_VALUE).maxCharacters(Integer.MAX_VALUE)
        .maxWhitespaceTokens(Integer.MAX_VALUE).maxRuleDepth(Integer.MAX_VALUE).build();
    Document doc = new Parser().parseDocument(graphql.parser.ParserEnvironment.newParserEnvironment()
        .document(Files.readString(Path.of(a[0]))).parserOptions(opts).build());
    for (var d : doc.getDefinitionsOfType(FragmentDefinition.class)) fragments.put(d.getName(), d);
    int depth = 0;
    for (var op : doc.getDefinitionsOfType(OperationDefinition.class))
      depth = Math.max(depth, depth(op.getSelectionSet(), new ArrayDeque<>()));
    System.out.printf("depth=%d fields=%d aliases=%d maxSameField=%d fragments=%d cycle=%s%n",
        depth, fields, aliases, maxSame, fragments.size(), cycle);
  }

  static int depth(SelectionSet set, Deque<String> path) {
    if (set == null) return 0;
    int best = 0;
    Map<String, Integer> same = new HashMap<>();
    for (Selection<?> s : set.getSelections()) {
      if (s instanceof Field f) {
        fields++;
        if (f.getAlias() != null) aliases++;
        maxSame = Math.max(maxSame, same.merge(f.getName(), 1, Integer::sum));
        best = Math.max(best, 1 + depth(f.getSelectionSet(), path));
      } else if (s instanceof InlineFragment i) {
        best = Math.max(best, depth(i.getSelectionSet(), path));
      } else if (s instanceof FragmentSpread sp) {
        if (path.contains(sp.getName())) { cycle = true; continue; }
        var def = fragments.get(sp.getName());
        if (def == null) continue;
        path.push(sp.getName());
        best = Math.max(best, depth(def.getSelectionSet(), path));
        path.pop();
      }
    }
    return best;
  }
}
