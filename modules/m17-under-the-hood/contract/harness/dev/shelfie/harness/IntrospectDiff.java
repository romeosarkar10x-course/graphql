package dev.shelfie.harness;

import dev.shelfie.mini.MiniGraphQL;
import dev.shelfie.mini.Resolver;
import dev.shelfie.mini.Schema;
import graphql.introspection.IntrospectionQueryBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The full introspection query (graphql-java's IntrospectionQueryBuilder, every option on) on both engines for one
 * SDL file; prints each difference as a JSON path. Types and directives are compared by name, not by position.
 */
public class IntrospectDiff {
    public static void main(String[] args) throws Exception {
        String sdl = Files.readString(Path.of(args[0]));
        String query = IntrospectionQueryBuilder.build(IntrospectionQueryBuilder.Options.defaultOptions()
                .descriptions(true).specifiedByUrl(true).directiveIsRepeatable(true).schemaDescription(true)
                .inputValueDeprecation(true).isOneOf(true).typeRefFragmentDepth(7));
        ObjectMapper json = new ObjectMapper();
        JsonNode mini = json.valueToTree(MiniGraphQL.execute(new Schema(sdl), query, null, null, Resolver.PROPERTIES, null));
        JsonNode gj = json.valueToTree(Gj.execute(Gj.schema(sdl, Resolver.PROPERTIES), query, null, null, null));
        List<String> diffs = new ArrayList<>();
        diff("", byName(mini), byName(gj), diffs);
        diffs.forEach(System.out::println);
        System.out.println(diffs.size() + " difference(s)");
    }

    /** Lists of named things (types, fields, directives...) become objects keyed by name, so order does not matter. */
    static JsonNode byName(JsonNode n) {
        if (n instanceof ArrayNode a && !a.isEmpty() && a.get(0).has("name") && a.get(0).get("name").isString() && !a.get(0).has("ofType")) {
            ObjectNode o = new ObjectMapper().createObjectNode();
            for (JsonNode x : a) o.set(x.get("name").asString(), byName(x));
            return o;
        }
        if (n instanceof ArrayNode a) { ArrayNode out = new ObjectMapper().createArrayNode(); a.forEach(x -> out.add(byName(x))); return out; }
        if (n instanceof ObjectNode o) { ObjectNode out = new ObjectMapper().createObjectNode(); o.properties().forEach(e -> out.set(e.getKey(), byName(e.getValue()))); return out; }
        return n;
    }

    static void diff(String path, JsonNode a, JsonNode b, List<String> out) {
        if (a == null || b == null) { out.add(path + ": " + (a == null ? "only graphql-java has " + b : "only mini has " + a)); return; }
        if (a.isObject() && b.isObject()) {
            var keys = new java.util.LinkedHashSet<String>();
            a.propertyNames().forEach(keys::add);
            b.propertyNames().forEach(keys::add);
            for (String k : keys) diff(path + "/" + k, a.get(k), b.get(k), out);
        } else if (a.isArray() && b.isArray() && a.size() == b.size()) {
            for (int i = 0; i < a.size(); i++) diff(path + "[" + i + "]", a.get(i), b.get(i), out);
        } else if (!a.equals(b)) out.add(path + ": mini " + a + " vs graphql-java " + b);
    }
}
