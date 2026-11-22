import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lab 12.5: a normalised GraphQL cache. The contract is modules/m12-clients/contract/cache/SPEC.md and
 * scenarios 01 to 10 there. Keep these four public signatures: the scenario runner calls them.
 *
 * graphql-java's parser is on the class path: Parser.parse(document) gives a Document; its
 * OperationDefinition has a SelectionSet of Fields, each with a name, an alias, arguments and its own
 * SelectionSet (null for a scalar).
 */
public final class NormalizedCache {
  private static final String ROOT = "ROOT_QUERY";
  private final Map<String, Map<String, Object>> store = new LinkedHashMap<>();

  public NormalizedCache(Map<String, Object> policies) {
  }

  /** Store a response: entities once each under Typename:id, root fields in ROOT_QUERY, references between them. */
  public void write(String document, Map<String, Object> variables, Map<String, Object> data) {
    // TODO
  }

  /** The document's data from the store, or null when any selected field is missing. */
  public Map<String, Object> read(String document, Map<String, Object> variables) {
    return null; // TODO
  }

  /** Every record, as SPEC.md shows them. */
  public Map<String, Object> extract() {
    return new LinkedHashMap<>(store);
  }
}
