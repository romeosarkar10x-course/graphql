package dev.shelfie.cli;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.TreeSet;

/**
 * Context-aware completion for a partial GraphQL document: the buffer up to the cursor is scanned once
 * (strings skipped), keeping a stack of the selection sets we are inside and their types. At the end we
 * know whether the cursor is at the top level, in a selection set of type T, in a field's argument list,
 * or after `... on`.
 */
final class Completion {
  private Completion() {}

  static List<String> complete(Schema schema, String buffer) {
    int end = buffer.length(), start = end;
    while (start > 0 && isName(buffer.charAt(start - 1))) start--;
    String prefix = buffer.substring(start);
    Scan s = scan(schema, buffer.substring(0, start));
    TreeSet<String> out = new TreeSet<>();
    if (s.afterOn) {
      out.addAll(schema.fragmentTargets(s.types.isEmpty() ? null : s.types.peek()));
    } else if (s.argsOf != null) {
      if (!s.afterColon) s.argsOf.args().forEach(a -> out.add(a.name()));
    } else if (!s.types.isEmpty()) {
      Schema.TypeInfo t = schema.type(s.types.peek());
      if (t != null) out.addAll(t.fields().keySet());
    } else {
      out.addAll(List.of("query", "mutation", "subscription", "fragment"));
    }
    out.removeIf(c -> !c.startsWith(prefix) || c.startsWith("__"));
    return List.copyOf(out);
  }

  static final class Scan {
    final Deque<String> types = new ArrayDeque<>();   // selection-set types, innermost on top
    Schema.FieldInfo argsOf;                          // inside this field's (...)
    boolean afterColon;                               // expecting an argument value, not a name
    int valueNest;                                    // inside a [list] or {object} argument value
    boolean afterOn;                                  // "... on " just before the cursor
  }

  static Scan scan(Schema schema, String text) {
    Scan s = new Scan();
    String rootType = schema.query;
    String lastField = null, pendingType = null, prevToken = null, prevPrev = null;
    int depth = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '"') {                                 // skip a string (block strings too)
        boolean block = text.startsWith("\"\"\"", i);
        int close = block ? text.indexOf("\"\"\"", i + 3) : nextQuote(text, i + 1);
        if (close < 0) { s.afterColon = true; return s; }
        i = block ? close + 2 : close;
        if (s.argsOf != null && s.valueNest == 0) s.afterColon = false;   // the value is complete
        prevPrev = prevToken; prevToken = "\"";
        continue;
      }
      if (c == '#') { int nl = text.indexOf('\n', i); if (nl < 0) break; i = nl; continue; }
      if (Character.isWhitespace(c) || c == ',') continue;
      if (isName(c)) {
        int j = i;
        while (j < text.length() && isName(text.charAt(j))) j++;
        String word = text.substring(i, j);
        i = j - 1;
        if (s.types.isEmpty() && s.argsOf == null) {
          if (word.equals("mutation")) rootType = schema.mutation;
          else if (word.equals("subscription")) rootType = schema.subscription;
          else if (word.equals("query")) rootType = schema.query;
        }
        if ("on".equals(prevToken) && "...".equals(prevPrev)) pendingType = word;
        else if (s.argsOf == null && !s.types.isEmpty() && !word.equals("on")) lastField = word;
        if (s.argsOf != null && s.valueNest == 0) s.afterColon = false;   // an arg name, or a whole value (42, true, ENUM)
        prevPrev = prevToken; prevToken = word;
        continue;
      }
      if (depth > 0) {                                // inside (...): braces and brackets are values
        switch (c) {
          case '{', '[' -> s.valueNest++;
          case '}', ']' -> { s.valueNest--; if (s.valueNest == 0) s.afterColon = false; }
          case ':' -> { if (s.valueNest == 0) s.afterColon = true; }
          case '$' -> { }
          case '(' -> depth++;
          case ')' -> { depth--; if (depth == 0) { s.argsOf = null; s.afterColon = false; s.valueNest = 0; } }
          default -> { }
        }
        prevPrev = prevToken; prevToken = String.valueOf(c);
        continue;
      }
      switch (c) {
        case '{' -> {
          String type;
          if (pendingType != null) type = pendingType;
          else if (s.types.isEmpty()) type = rootType;
          else type = fieldType(schema, s.types.peek(), lastField);
          s.types.push(type == null ? "?" : type);
          pendingType = null; lastField = null;
        }
        case '}' -> { if (!s.types.isEmpty()) s.types.pop(); lastField = null; }
        case '(' -> {
          depth++;
          if (depth == 1 && !s.types.isEmpty() && lastField != null) {
            Schema.TypeInfo t = schema.type(s.types.peek());
            s.argsOf = t == null ? null : t.fields().get(lastField);
          }
        }
        case '.' -> {
          if (text.startsWith("...", i)) { i += 2; prevPrev = prevToken; prevToken = "..."; continue; }
        }
        default -> { }
      }
      prevPrev = prevToken; prevToken = String.valueOf(c);
    }
    s.afterOn = "on".equals(prevToken) && "...".equals(prevPrev);
    return s;
  }

  private static String fieldType(Schema schema, String parent, String field) {
    Schema.TypeInfo t = schema.type(parent);
    if (t == null || field == null) return null;
    Schema.FieldInfo f = t.fields().get(field);
    return f == null ? null : f.namedType();
  }

  private static int nextQuote(String text, int from) {
    for (int i = from; i < text.length(); i++) {
      if (text.charAt(i) == '\\') { i++; continue; }
      if (text.charAt(i) == '"') return i;
    }
    return -1;
  }

  static boolean isName(char c) {
    return c == '_' || Character.isLetterOrDigit(c);
  }
}
