import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the cache scenarios (SPEC.md) against NormalizedCache.java in the same directory.
 *   java -cp <lib> Scenarios.java <scenario dir> [first-last]     e.g. 01-10
 * Prints PASS/FAIL per scenario; a FAIL line names the op that failed, what was expected and what came back.
 */
public class Scenarios {
  static final JsonMapper JSON = JsonMapper.builder().build();

  static final class Watch {
    int count;
    Object last;
    Runnable cancel;
  }

  public static void main(String[] args) throws Exception {
    Path dir = Path.of(args[0]);
    int from = 1, to = 99;
    if (args.length > 1) {
      String[] r = args[1].split("-");
      from = Integer.parseInt(r[0]);
      to = Integer.parseInt(r[1]);
    }
    List<Path> files;
    try (Stream<Path> s = Files.list(dir)) {
      files = s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
    }
    int failed = 0, ran = 0;
    for (Path f : files) {
      String name = f.getFileName().toString().replace(".json", "");
      int n = Integer.parseInt(name.substring(0, 2));
      if (n < from || n > to) continue;
      ran++;
      String problem;
      try {
        problem = run(JSON.readTree(Files.readString(f)));
      } catch (Throwable t) {
        problem = "threw " + t;
      }
      if (problem == null) System.out.println("PASS " + name);
      else { System.out.println("FAIL " + name + "\t" + problem.replace('\n', ' ')); failed++; }
    }
    if (ran == 0) { System.out.println("FAIL no scenarios in " + dir); failed++; }
    System.exit(failed == 0 ? 0 : 1);
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> map(JsonNode n) {
    return n == null || n.isNull() || n.isMissingNode() ? new HashMap<>() : JSON.convertValue(n, Map.class);
  }

  /** Values as the learner returned them, normalised through JSON so 4 and 4L compare equal. */
  static JsonNode tree(Object v) {
    return JSON.readTree(JSON.writeValueAsString(v));
  }

  /**
   * The mini-project's operations, found by name so lab 12.5's cache (write, read, extract only) compiles
   * with this runner. A missing method fails that scenario.
   */
  static Object call(Object target, String name, Object... args) {
    for (java.lang.reflect.Method m : target.getClass().getMethods()) {
      if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
      try {
        return m.invoke(target, args);
      } catch (java.lang.reflect.InvocationTargetException e) {
        throw e.getCause() instanceof RuntimeException r ? r : new RuntimeException(e.getCause());
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
    }
    throw new UnsupportedOperationException("NormalizedCache has no public " + name + " with " + args.length + " parameter(s)");
  }

  static String run(JsonNode scenario) {
    NormalizedCache cache = new NormalizedCache(map(scenario.get("policies")));
    Map<String, Watch> watches = new HashMap<>();
    int i = 0;
    for (JsonNode op : scenario.path("ops")) {
      i++;
      String kind = op.path("op").asString();
      String where = "op " + i + " (" + kind + ")";
      switch (kind) {
        case "write" -> cache.write(op.path("query").asString(), map(op.get("variables")), map(op.get("data")));
        case "writeOptimistic" -> call(cache, "writeOptimistic", op.path("layer").asString(), op.path("query").asString(), map(op.get("variables")), map(op.get("data")));
        case "removeOptimistic" -> call(cache, "removeOptimistic", op.path("layer").asString());
        case "read" -> {
          JsonNode got = tree(cache.read(op.path("query").asString(), map(op.get("variables"))));
          JsonNode want = op.get("expect");
          if (!got.equals(want)) return where + ": want " + (want.isNull() ? "a miss (null)" : want) + ", got " + (got.isNull() ? "a miss (null)" : got);
        }
        case "extract" -> {
          JsonNode got = tree(cache.extract());
          if (!got.equals(op.get("expect"))) return where + ": want " + op.get("expect") + ", got " + got;
        }
        case "evict" -> {
          if (op.has("field")) call(cache, "evict", op.path("id").asString(), op.path("field").asString());
          else call(cache, "evict", op.path("id").asString());
        }
        case "gc" -> {
          JsonNode got = tree(call(cache, "gc"));
          if (!got.equals(op.get("expect"))) return where + ": want " + op.get("expect") + " removed, got " + got;
        }
        case "watch" -> {
          Watch w = new Watch();
          java.util.function.Consumer<Map<String, Object>> listener = result -> {
            w.count++;
            w.last = result;
          };
          w.cancel = (Runnable) call(cache, "watch", op.path("query").asString(), map(op.get("variables")), listener);
          watches.put(op.path("name").asString(), w);
        }
        case "unwatch" -> watches.get(op.path("name").asString()).cancel.run();
        case "notified" -> {
          Watch w = watches.get(op.path("name").asString());
          int want = op.path("count").asInt();
          if (w.count != want) return where + ": want the listener to have run " + want + " time(s), it ran " + w.count;
          if (op.has("last") && !tree(w.last).equals(op.get("last"))) return where + ": last value want " + op.get("last") + ", got " + tree(w.last);
        }
        default -> throw new IllegalArgumentException("unknown op " + kind);
      }
    }
    return null;
  }
}
