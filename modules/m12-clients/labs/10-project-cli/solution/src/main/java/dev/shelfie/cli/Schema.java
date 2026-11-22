package dev.shelfie.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;

/** What completion needs from introspection: each type's fields (with arguments and result type) and possible types. */
final class Schema {
  record Arg(String name, String type) {}
  record FieldInfo(String name, String type, String namedType, List<Arg> args, String description) {}
  record TypeInfo(String name, String kind, Map<String, FieldInfo> fields, List<String> possibleTypes) {}

  static final String INTROSPECTION = """
      query CliIntrospection {
        __schema {
          queryType { name } mutationType { name } subscriptionType { name }
          types {
            name kind
            possibleTypes { name }
            fields { name description args { name type { ...T } } type { ...T } }
          }
        }
      }
      fragment T on __Type { kind name ofType { kind name ofType { kind name ofType { kind name } } } }
      """;

  final String query, mutation, subscription;
  final Map<String, TypeInfo> types = new LinkedHashMap<>();

  Schema(JsonNode schema) {
    query = schema.path("queryType").path("name").asString(null);
    mutation = schema.path("mutationType").path("name").asString(null);
    subscription = schema.path("subscriptionType").path("name").asString(null);
    for (JsonNode t : schema.path("types")) {
      Map<String, FieldInfo> fields = new LinkedHashMap<>();
      for (JsonNode f : t.path("fields")) {
        List<Arg> args = new ArrayList<>();
        for (JsonNode a : f.path("args")) args.add(new Arg(a.path("name").asString(), render(a.path("type"))));
        fields.put(f.path("name").asString(), new FieldInfo(f.path("name").asString(), render(f.path("type")), named(f.path("type")), args, f.path("description").asString(null)));
      }
      List<String> possible = new ArrayList<>();
      for (JsonNode p : t.path("possibleTypes")) possible.add(p.path("name").asString());
      types.put(t.path("name").asString(), new TypeInfo(t.path("name").asString(), t.path("kind").asString(), fields, possible));
    }
  }

  static Schema load(Http http) {
    JsonNode r = http.execute(INTROSPECTION);
    if (r.path("data").path("__schema").isMissingNode()) throw new IllegalStateException("introspection failed: " + r.path("errors"));
    return new Schema(r.path("data").path("__schema"));
  }

  TypeInfo type(String name) {
    return name == null ? null : types.get(name);
  }

  /** Object types an inline fragment inside `name` may name: itself, or a union's or interface's members. */
  TreeSet<String> fragmentTargets(String name) {
    TreeSet<String> out = new TreeSet<>();
    TypeInfo t = type(name);
    if (t == null) return out;
    if (t.possibleTypes().isEmpty()) out.add(t.name());
    else out.addAll(t.possibleTypes());
    return out;
  }

  private static String named(JsonNode t) {
    while (t.path("name").isNull() || t.path("name").isMissingNode()) t = t.path("ofType");
    return t.path("name").asString();
  }

  private static String render(JsonNode t) {
    return switch (t.path("kind").asString()) {
      case "NON_NULL" -> render(t.path("ofType")) + "!";
      case "LIST" -> "[" + render(t.path("ofType")) + "]";
      default -> t.path("name").asString();
    };
  }
}
