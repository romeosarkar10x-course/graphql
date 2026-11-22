import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.Map;

/**
 * Mini-project 12.9: the normalised cache, all 25 scenarios of modules/m12-clients/contract/cache/.
 * Start from your lab 12.5 NormalizedCache.java (paste it over this file), then add the methods below.
 * Keep the public signatures: the scenario runner calls them.
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

  public void evict(String id) {
    // TODO scenario 20
  }

  public void evict(String id, String fieldKey) {
    // TODO scenario 21
  }

  /** Removes records ROOT_QUERY cannot reach; returns their ids, sorted. */
  public List<String> gc() {
    return List.of(); // TODO scenario 22
  }

  public void writeOptimistic(String layer, String document, Map<String, Object> variables, Map<String, Object> data) {
    // TODO scenarios 23, 24
  }

  public void removeOptimistic(String layer) {
    // TODO
  }

  /** Calls the listener when this document's read result changes; the Runnable cancels. */
  public Runnable watch(String document, Map<String, Object> variables, Consumer<Map<String, Object>> listener) {
    return () -> { }; // TODO scenario 25
  }
}
