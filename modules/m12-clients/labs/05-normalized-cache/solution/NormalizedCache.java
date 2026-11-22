import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.Field;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.language.VariableReference;
import graphql.parser.Parser;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 12.5: a normalised GraphQL cache (contract/cache/SPEC.md, scenarios 01 to 10). Records are flat maps
 * keyed by cache id (Book:b1, ROOT_QUERY); an entity inside a record is {"__ref": "Book:b1"}, so each entity
 * is stored once and every query that shows it reads the same record.
 */
public final class NormalizedCache {
  private static final String ROOT = "ROOT_QUERY";
  /** A read that needs a field the store does not have. */
  private static final Object MISS = new Object();
  /** A variable the request did not send: its argument is left out of the key. */
  private static final Object ABSENT = new Object();
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final Map<String, Map<String, Object>> store = new LinkedHashMap<>();
  private final Map<String, OperationDefinition> parsed = new HashMap<>();

  public NormalizedCache(Map<String, Object> policies) {
    // Lab 12.5 has no policies; mini-project 12.9 adds them.
  }

  // ---------------------------------------------------------------- writing

  public void write(String document, Map<String, Object> variables, Map<String, Object> data) {
    OperationDefinition op = parse(document);
    // Query root fields go in ROOT_QUERY. Mutation root fields are not stored, but the entities in them are:
    // that is how one mutation result updates every cached query that shows the same book.
    Map<String, Object> root = op.getOperation() == OperationDefinition.Operation.QUERY
        ? store.computeIfAbsent(ROOT, k -> new LinkedHashMap<>())
        : new LinkedHashMap<>();
    writeSelection(op.getSelectionSet(), data, root, variables);
  }

  private void writeSelection(SelectionSet set, Map<String, Object> object, Map<String, Object> into, Map<String, Object> vars) {
    for (Selection<?> selection : set.getSelections()) {
      if (!(selection instanceof Field f)) continue;
      String responseKey = f.getResultKey();                 // the alias, or the name
      if (!object.containsKey(responseKey)) continue;
      into.put(storeKey(f, vars), normalize(object.get(responseKey), f.getSelectionSet(), vars));
    }
  }

  private Object normalize(Object value, SelectionSet set, Map<String, Object> vars) {
    if (value == null) return null;
    if (value instanceof List<?> list) {
      List<Object> out = new ArrayList<>();
      for (Object item : list) out.add(normalize(item, set, vars));
      return out;
    }
    if (set == null || !(value instanceof Map<?, ?>)) return copy(value);   // a scalar
    Map<String, Object> object = cast(value);
    String id = identify(object);
    if (id == null) {                                         // no id: embedded, replaced wholesale
      Map<String, Object> embedded = new LinkedHashMap<>();
      writeSelection(set, object, embedded, vars);
      return embedded;
    }
    Map<String, Object> rec = store.computeIfAbsent(id, k -> new LinkedHashMap<>());   // merge into the record
    writeSelection(set, object, rec, vars);
    Map<String, Object> ref = new LinkedHashMap<>();
    ref.put("__ref", id);
    return ref;
  }

  private static String identify(Map<String, Object> object) {
    Object typename = object.get("__typename");
    Object id = object.get("id");
    return typename instanceof String t && id != null ? t + ":" + id : null;
  }

  // ---------------------------------------------------------------- reading

  public Map<String, Object> read(String document, Map<String, Object> variables) {
    OperationDefinition op = parse(document);
    if (op.getOperation() != OperationDefinition.Operation.QUERY) return null;
    Object result = readSelection(op.getSelectionSet(), store.get(ROOT), variables);
    return result == MISS ? null : cast(result);
  }

  private Object readSelection(SelectionSet set, Map<String, Object> record, Map<String, Object> vars) {
    if (record == null) return MISS;
    Map<String, Object> out = new LinkedHashMap<>();
    for (Selection<?> selection : set.getSelections()) {
      if (!(selection instanceof Field f)) continue;
      String key = storeKey(f, vars);
      if (!record.containsKey(key)) return MISS;              // a stored null is a value, a missing key is not
      Object value = denormalize(record.get(key), f.getSelectionSet(), vars);
      if (value == MISS) return MISS;
      out.put(f.getResultKey(), value);
    }
    return out;
  }

  private Object denormalize(Object value, SelectionSet set, Map<String, Object> vars) {
    if (value == null) return null;
    if (value instanceof List<?> list) {
      List<Object> out = new ArrayList<>();
      for (Object item : list) {
        Object read = denormalize(item, set, vars);
        if (read == MISS) return MISS;
        out.add(read);
      }
      return out;
    }
    if (set == null) return copy(value);
    Map<String, Object> object = cast(value);
    if (object.size() == 1 && object.get("__ref") instanceof String id) return readSelection(set, store.get(id), vars);
    return readSelection(set, object, vars);
  }

  public Map<String, Object> extract() {
    return cast(copy(store));
  }

  // ---------------------------------------------------------------- keys

  private OperationDefinition parse(String document) {
    return parsed.computeIfAbsent(document, text -> Parser.parse(text).getDefinitionsOfType(OperationDefinition.class).getFirst());
  }

  /** name, or name({"a":1,"b":"x"}): arguments sorted by name, variables replaced by their values. */
  private static String storeKey(Field f, Map<String, Object> vars) {
    Map<String, Object> args = new TreeMap<>();
    for (var a : f.getArguments()) {
      Object v = resolve(a.getValue(), vars);
      if (v != ABSENT) args.put(a.getName(), v);
    }
    return args.isEmpty() ? f.getName() : f.getName() + "(" + JSON.writeValueAsString(args) + ")";
  }

  private static Object resolve(Value<?> value, Map<String, Object> vars) {
    return switch (value) {
      case VariableReference v -> vars.containsKey(v.getName()) ? sorted(vars.get(v.getName())) : ABSENT;
      case StringValue s -> s.getValue();
      case IntValue i -> {
        BigInteger n = i.getValue();
        yield n.bitLength() < 32 ? (Object) n.intValue() : (Object) n.longValue();
      }
      case FloatValue fl -> fl.getValue().doubleValue();
      case BooleanValue b -> b.isValue();
      case NullValue n -> null;
      case EnumValue e -> e.getName();
      case ArrayValue arr -> arr.getValues().stream().map(v -> resolve(v, vars)).toList();
      case ObjectValue obj -> {
        Map<String, Object> m = new TreeMap<>();
        for (ObjectField of : obj.getObjectFields()) {
          Object v = resolve(of.getValue(), vars);
          if (v != ABSENT) m.put(of.getName(), v);
        }
        yield m;
      }
      default -> throw new IllegalArgumentException("unsupported value " + value);
    };
  }

  private static Object sorted(Object value) {
    if (value instanceof Map<?, ?> m) {
      Map<String, Object> out = new TreeMap<>();
      m.forEach((k, v) -> out.put(String.valueOf(k), sorted(v)));
      return out;
    }
    if (value instanceof List<?> l) return l.stream().map(NormalizedCache::sorted).toList();
    return value;
  }

  private static Object copy(Object value) {
    if (value instanceof Map<?, ?> m) {
      Map<String, Object> out = new LinkedHashMap<>();
      m.forEach((k, v) -> out.put(String.valueOf(k), copy(v)));
      return out;
    }
    if (value instanceof List<?> l) {
      List<Object> out = new ArrayList<>();
      for (Object v : l) out.add(copy(v));
      return out;
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> cast(Object value) {
    return (Map<String, Object>) value;
  }
}
