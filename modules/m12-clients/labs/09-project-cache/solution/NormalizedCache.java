import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.Directive;
import graphql.language.DirectivesContainer;
import graphql.language.Document;
import graphql.language.EnumValue;
import graphql.language.Field;
import graphql.language.FloatValue;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Mini-project 12.9: a normalised GraphQL cache (contract/cache/SPEC.md). Records are flat maps keyed by
 * cache id; entities inside them are {"__ref": id}. No schema: everything it knows comes from the documents,
 * the responses (__typename, id) and the policies.
 */
public final class NormalizedCache {
  private static final String ROOT = "ROOT_QUERY";
  private static final Object MISS = new Object();
  private static final Object ABSENT = new Object();
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private record Parsed(OperationDefinition operation, Map<String, FragmentDefinition> fragments) {}

  private static final class Watch {
    final String document;
    final Map<String, Object> variables;
    final Consumer<Map<String, Object>> listener;
    Map<String, Object> last;

    Watch(String document, Map<String, Object> variables, Consumer<Map<String, Object>> listener) {
      this.document = document;
      this.variables = variables;
      this.listener = listener;
    }
  }

  private final Map<String, Map<String, Object>> base = new LinkedHashMap<>();
  /** Optimistic layers in the order they were first written; each holds partial records. */
  private final LinkedHashMap<String, Map<String, Map<String, Object>>> layers = new LinkedHashMap<>();
  private final Map<String, Parsed> parsed = new HashMap<>();
  private final List<Watch> watches = new CopyOnWriteArrayList<>();
  private final Map<String, Set<String>> possibleTypes = new HashMap<>();
  private final Map<String, List<String>> keyFields = new HashMap<>();
  private final Map<String, Map<String, Object>> fieldPolicies = new HashMap<>();

  @SuppressWarnings("unchecked")
  public NormalizedCache(Map<String, Object> policies) {
    ((Map<String, List<String>>) policies.getOrDefault("possibleTypes", Map.of()))
        .forEach((abstractType, members) -> possibleTypes.put(abstractType, new HashSet<>(members)));
    ((Map<String, Map<String, Object>>) policies.getOrDefault("types", Map.of()))
        .forEach((type, p) -> { if (p.containsKey("keyFields")) keyFields.put(type, (List<String>) p.get("keyFields")); });
    fieldPolicies.putAll((Map<String, Map<String, Object>>) policies.getOrDefault("fields", Map.of()));
  }

  // ---------------------------------------------------------------- public API

  public void write(String document, Map<String, Object> variables, Map<String, Object> data) {
    writeInto(base, document, variables, data);
    notifyWatches();
  }

  public Map<String, Object> read(String document, Map<String, Object> variables) {
    Parsed p = parse(document);
    if (p.operation().getOperation() != OperationDefinition.Operation.QUERY) return null;
    Object r = readSelection(p.operation().getSelectionSet(), ROOT, null, "Query", variables, p.fragments());
    return r == MISS ? null : cast(r);
  }

  public Map<String, Object> extract() {
    return cast(copy(base));
  }

  public void evict(String id) {
    base.remove(id);
    layers.values().forEach(layer -> layer.remove(id));
    notifyWatches();
  }

  public void evict(String id, String fieldKey) {
    Map<String, Object> rec = base.get(id);
    if (rec != null) rec.remove(fieldKey);
    for (Map<String, Map<String, Object>> layer : layers.values()) {
      Map<String, Object> r = layer.get(id);
      if (r != null) r.remove(fieldKey);
    }
    notifyWatches();
  }

  public List<String> gc() {
    Set<String> reachable = new HashSet<>();
    Deque<String> todo = new ArrayDeque<>(List.of(ROOT));
    while (!todo.isEmpty()) {
      String id = todo.pop();
      if (!reachable.add(id)) continue;
      for (Map<String, Object> rec : versions(id)) collectRefs(rec, todo);
    }
    Set<String> all = new HashSet<>(base.keySet());
    layers.values().forEach(layer -> all.addAll(layer.keySet()));
    List<String> removed = new ArrayList<>();
    for (String id : all) {
      if (reachable.contains(id)) continue;
      base.remove(id);
      layers.values().forEach(layer -> layer.remove(id));
      removed.add(id);
    }
    Collections.sort(removed);
    if (!removed.isEmpty()) notifyWatches();
    return removed;
  }

  public void writeOptimistic(String layer, String document, Map<String, Object> variables, Map<String, Object> data) {
    writeInto(layers.computeIfAbsent(layer, k -> new LinkedHashMap<>()), document, variables, data);
    notifyWatches();
  }

  public void removeOptimistic(String layer) {
    if (layers.remove(layer) != null) notifyWatches();
  }

  public Runnable watch(String document, Map<String, Object> variables, Consumer<Map<String, Object>> listener) {
    Watch w = new Watch(document, variables, listener);
    w.last = read(document, variables);
    watches.add(w);
    return () -> watches.remove(w);
  }

  // ---------------------------------------------------------------- writing

  private void writeInto(Map<String, Map<String, Object>> target, String document, Map<String, Object> variables, Map<String, Object> data) {
    Parsed p = parse(document);
    boolean query = p.operation().getOperation() == OperationDefinition.Operation.QUERY;
    // Mutation and subscription root fields are not stored; the entities inside them are.
    Map<String, Object> root = query ? target.computeIfAbsent(ROOT, k -> new LinkedHashMap<>()) : new LinkedHashMap<>();
    String rootType = query ? "Query" : p.operation().getOperation() == OperationDefinition.Operation.MUTATION ? "Mutation" : "Subscription";
    writeSelection(target, p.operation().getSelectionSet(), rootType, query ? ROOT : null, data, root, variables, p.fragments());
  }

  private void writeSelection(Map<String, Map<String, Object>> target, SelectionSet set, String typename, String recId,
                              Map<String, Object> object, Map<String, Object> into, Map<String, Object> vars,
                              Map<String, FragmentDefinition> fragments) {
    for (Selection<?> selection : set.getSelections()) {
      if (selection instanceof DirectivesContainer<?> d && !included(d, vars)) continue;
      switch (selection) {
        case Field f -> {
          String responseKey = f.getResultKey();
          if (!object.containsKey(responseKey)) continue;
          String key = storeKey(typename, f, vars);
          Object value = normalize(target, object.get(responseKey), f.getSelectionSet(), vars, fragments);
          Map<String, Object> policy = fieldPolicies.get(typename + "." + f.getName());
          if (policy != null && "append".equals(policy.get("merge")) && value instanceof List<?> page) {
            Object existing = recId != null ? lookup(recId, key) : into.getOrDefault(key, MISS);
            if (existing instanceof List<?> earlier) {
              List<Object> merged = new ArrayList<>(earlier);
              merged.addAll(page);
              value = merged;
            }
          }
          into.put(key, value);
        }
        case InlineFragment inline -> {
          String condition = inline.getTypeCondition() == null ? null : inline.getTypeCondition().getName();
          if (applies(condition, actualType(object, typename)))
            writeSelection(target, inline.getSelectionSet(), typename, recId, object, into, vars, fragments);
        }
        case FragmentSpread spread -> {
          FragmentDefinition def = fragments.get(spread.getName());
          if (def != null && applies(def.getTypeCondition().getName(), actualType(object, typename)))
            writeSelection(target, def.getSelectionSet(), typename, recId, object, into, vars, fragments);
        }
        default -> { }
      }
    }
  }

  private Object normalize(Map<String, Map<String, Object>> target, Object value, SelectionSet set,
                           Map<String, Object> vars, Map<String, FragmentDefinition> fragments) {
    if (value == null) return null;
    if (value instanceof List<?> list) {
      List<Object> out = new ArrayList<>();
      for (Object item : list) out.add(normalize(target, item, set, vars, fragments));
      return out;
    }
    if (set == null || !(value instanceof Map<?, ?>)) return copy(value);   // a scalar, or a JSON scalar
    Map<String, Object> object = cast(value);
    String typename = (String) object.get("__typename");
    String id = identify(object);
    if (id != null) {
      Map<String, Object> rec = target.computeIfAbsent(id, k -> new LinkedHashMap<>());
      writeSelection(target, set, typename, id, object, rec, vars, fragments);
      return reference(id);
    }
    Map<String, Object> embedded = new LinkedHashMap<>();
    writeSelection(target, set, typename, null, object, embedded, vars, fragments);
    return embedded;
  }

  /** Book:b1, or Edition:{"isbn":"..."} with keyFields; null for an object that is not an entity. */
  private String identify(Map<String, Object> object) {
    Object typename = object.get("__typename");
    if (!(typename instanceof String type)) return null;
    List<String> fields = keyFields.get(type);
    if (fields != null) {
      Map<String, Object> key = new TreeMap<>();
      for (String f : fields) {
        if (!object.containsKey(f)) return null;
        key.put(f, object.get(f));
      }
      return type + ":" + JSON.writeValueAsString(key);
    }
    Object id = object.get("id");
    return id == null ? null : type + ":" + id;
  }

  // ---------------------------------------------------------------- reading

  private Object readSelection(SelectionSet set, String recId, Map<String, Object> embedded, String typename,
                               Map<String, Object> vars, Map<String, FragmentDefinition> fragments) {
    Map<String, Object> out = new LinkedHashMap<>();
    for (Selection<?> selection : set.getSelections()) {
      if (selection instanceof DirectivesContainer<?> d && !included(d, vars)) continue;
      Object part;
      switch (selection) {
        case Field f -> {
          String key = storeKey(typename, f, vars);
          Object value = recId != null ? lookup(recId, key) : embedded.containsKey(key) ? embedded.get(key) : MISS;
          if (value == MISS) value = redirect(typename, f, vars);
          if (value == MISS) return MISS;
          Object read = denormalize(value, f.getSelectionSet(), vars, fragments);
          if (read == MISS) return MISS;
          out.put(f.getResultKey(), read);
          continue;
        }
        case InlineFragment inline -> {
          String condition = inline.getTypeCondition() == null ? null : inline.getTypeCondition().getName();
          if (!applies(condition, typename)) continue;
          part = readSelection(inline.getSelectionSet(), recId, embedded, typename, vars, fragments);
        }
        case FragmentSpread spread -> {
          FragmentDefinition def = fragments.get(spread.getName());
          if (def == null || !applies(def.getTypeCondition().getName(), typename)) continue;
          part = readSelection(def.getSelectionSet(), recId, embedded, typename, vars, fragments);
        }
        default -> { continue; }
      }
      if (part == MISS) return MISS;
      out.putAll(cast(part));
    }
    return out;
  }

  private Object denormalize(Object value, SelectionSet set, Map<String, Object> vars, Map<String, FragmentDefinition> fragments) {
    if (value == null) return null;
    if (value instanceof List<?> list) {
      List<Object> out = new ArrayList<>();
      for (Object item : list) {
        Object read = denormalize(item, set, vars, fragments);
        if (read == MISS) return MISS;
        out.add(read);
      }
      return out;
    }
    if (set == null) return copy(value);
    Map<String, Object> object = cast(value);
    if (object.size() == 1 && object.get("__ref") instanceof String id) {
      if (!exists(id)) return MISS;
      Object typename = lookup(id, "__typename");
      return readSelection(set, id, null, typename instanceof String t ? t : null, vars, fragments);
    }
    return readSelection(set, null, object, (String) object.get("__typename"), vars, fragments);
  }

  /** read: toReference. ROOT_QUERY has no book({"id":"b2"}), but Book:b2 exists: use it. */
  private Object redirect(String typename, Field f, Map<String, Object> vars) {
    Map<String, Object> policy = fieldPolicies.get(typename + "." + f.getName());
    if (policy == null || !"toReference".equals(policy.get("read"))) return MISS;
    Object id = arguments(f, vars).get((String) policy.get("idArg"));
    if (id == null) return MISS;
    String ref = policy.get("typename") + ":" + id;
    return exists(ref) ? reference(ref) : MISS;
  }

  // ---------------------------------------------------------------- the layered store

  private Object lookup(String id, String key) {
    List<Map<String, Map<String, Object>>> stack = new ArrayList<>(layers.values());
    Collections.reverse(stack);
    for (Map<String, Map<String, Object>> layer : stack) {
      Map<String, Object> rec = layer.get(id);
      if (rec != null && rec.containsKey(key)) return rec.get(key);
    }
    Map<String, Object> rec = base.get(id);
    return rec != null && rec.containsKey(key) ? rec.get(key) : MISS;
  }

  private boolean exists(String id) {
    return base.containsKey(id) || layers.values().stream().anyMatch(layer -> layer.containsKey(id));
  }

  private List<Map<String, Object>> versions(String id) {
    List<Map<String, Object>> out = new ArrayList<>();
    if (base.containsKey(id)) out.add(base.get(id));
    for (Map<String, Map<String, Object>> layer : layers.values()) if (layer.containsKey(id)) out.add(layer.get(id));
    return out;
  }

  private static void collectRefs(Object value, Deque<String> into) {
    if (value instanceof List<?> list) list.forEach(v -> collectRefs(v, into));
    else if (value instanceof Map<?, ?> map) {
      if (map.size() == 1 && map.get("__ref") instanceof String id) into.push(id);
      else map.values().forEach(v -> collectRefs(v, into));
    }
  }

  private void notifyWatches() {
    for (Watch w : watches) {
      Map<String, Object> now = read(w.document, w.variables);
      if (!Objects.equals(now, w.last)) {
        w.last = now;
        w.listener.accept(now);
      }
    }
  }

  // ---------------------------------------------------------------- documents, keys, directives

  private Parsed parse(String document) {
    return parsed.computeIfAbsent(document, text -> {
      Document doc = Parser.parse(text);
      OperationDefinition op = doc.getDefinitionsOfType(OperationDefinition.class).getFirst();
      Map<String, FragmentDefinition> fragments = new HashMap<>();
      for (FragmentDefinition f : doc.getDefinitionsOfType(FragmentDefinition.class)) fragments.put(f.getName(), f);
      return new Parsed(op, fragments);
    });
  }

  /** name, or name({"a":1,"b":"x"}): arguments sorted, variables resolved, absent variables dropped, keyArgs applied. */
  private String storeKey(String typename, Field f, Map<String, Object> vars) {
    Map<String, Object> args = arguments(f, vars);
    Map<String, Object> policy = fieldPolicies.get(typename + "." + f.getName());
    if (policy != null && policy.get("keyArgs") instanceof List<?> keep) args.keySet().retainAll(keep);
    return args.isEmpty() ? f.getName() : f.getName() + "(" + JSON.writeValueAsString(args) + ")";
  }

  private static Map<String, Object> arguments(Field f, Map<String, Object> vars) {
    Map<String, Object> args = new TreeMap<>();
    for (var a : f.getArguments()) {
      Object v = resolve(a.getValue(), vars);
      if (v != ABSENT) args.put(a.getName(), v);
    }
    return args;
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

  private static boolean included(DirectivesContainer<?> node, Map<String, Object> vars) {
    for (Directive d : node.getDirectives()) {
      if (!d.getName().equals("include") && !d.getName().equals("skip")) continue;
      Object condition = d.getArgument("if") == null ? null : resolve(d.getArgument("if").getValue(), vars);
      boolean on = Boolean.TRUE.equals(condition);
      if (d.getName().equals("include") && !on) return false;
      if (d.getName().equals("skip") && on) return false;
    }
    return true;
  }

  private boolean applies(String typeCondition, String typename) {
    if (typeCondition == null) return true;
    if (typename == null) return false;
    return typeCondition.equals(typename) || possibleTypes.getOrDefault(typeCondition, Set.of()).contains(typename);
  }

  private static String actualType(Map<String, Object> object, String fallback) {
    return object.get("__typename") instanceof String t ? t : fallback;
  }

  private static Map<String, Object> reference(String id) {
    Map<String, Object> ref = new LinkedHashMap<>();
    ref.put("__ref", id);
    return ref;
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
