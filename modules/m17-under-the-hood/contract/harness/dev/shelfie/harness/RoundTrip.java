package dev.shelfie.harness;

import dev.shelfie.mini.MiniGraphQL;
import dev.shelfie.mini.Printer;
import dev.shelfie.mini.Resolver;
import dev.shelfie.mini.Schema;
import graphql.introspection.IntrospectionQueryBuilder;
import graphql.introspection.IntrospectionResultToSchema;
import graphql.schema.idl.SchemaPrinter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Lab 17.6, for each SDL file:
 *   print     your printer's SDL describes the same schema (both printed by graphql-java's SchemaPrinter)
 *   stable    printing your printed SDL again changes nothing
 *   introspect your introspection of the user's types and the spec's directives equals graphql-java's
 *   rebuild   SDL rebuilt from your introspection equals SDL rebuilt from graphql-java's (both by graphql-java's
 *             IntrospectionResultToSchema, which cannot restore applied directives and also drops @oneOf and
 *             deprecated arguments, so it is not compared with the original)
 * Prints "pass <check> <file>" or "FAIL <check> <file>: ..." per check.
 */
public final class RoundTrip {
    static final ObjectMapper JSON = new ObjectMapper();
    static final Set<String> SPEC_DIRECTIVES = Set.of("include", "skip", "deprecated", "specifiedBy", "oneOf");
    static final Set<String> BUILT_IN_TYPES = Set.of("Int", "Float", "String", "Boolean", "ID");

    public static void main(String[] a) throws Exception {
        String query = IntrospectionQueryBuilder.build(IntrospectionQueryBuilder.Options.defaultOptions()
                .descriptions(true).specifiedByUrl(true).directiveIsRepeatable(true).schemaDescription(true)
                .inputValueDeprecation(true).isOneOf(true).typeRefFragmentDepth(7));
        for (String file : a) {
            String name = Path.of(file).getFileName().toString(), sdl = Files.readString(Path.of(file));
            String original = gjPrint(sdl);
            String printed = null;
            try {
                printed = Printer.schema(new Schema(sdl));
                report("print", name, original.equals(gjPrint(printed)) ? null : firstLineDiff(original, gjPrint(printed)));
                report("stable", name, printed.equals(Printer.schema(new Schema(printed))) ? null : firstLineDiff(printed, Printer.schema(new Schema(printed))));
            } catch (RuntimeException e) {
                report("print", name, "threw " + e + (printed == null ? "" : "\n      your SDL:\n" + printed.indent(8)));
            }
            try {
                Map<String, Object> mine = MiniGraphQL.execute(new Schema(sdl), query, null, null, Resolver.PROPERTIES, null);
                Map<String, Object> theirs = Gj.execute(Gj.schema(sdl, Resolver.PROPERTIES), query, null, null, null);
                List<String> diffs = new ArrayList<>();
                IntrospectDiff.diff("", comparable(JSON.valueToTree(mine)), comparable(JSON.valueToTree(theirs)), diffs);
                report("introspect", name, diffs.isEmpty() ? null : diffs.size() + " difference(s), first: " + String.join("\n      ", diffs.subList(0, Math.min(5, diffs.size()))));
                @SuppressWarnings("unchecked")
                var doc = new IntrospectionResultToSchema().createSchemaDefinition((Map<String, Object>) mine.get("data"));
                String rebuilt = gjPrint(graphql.language.AstPrinter.printAst(doc));
                @SuppressWarnings("unchecked")
                var own = new IntrospectionResultToSchema().createSchemaDefinition((Map<String, Object>) theirs.get("data"));
                String expected = gjPrint(graphql.language.AstPrinter.printAst(own));
                report("rebuild", name, expected.equals(rebuilt) ? null : firstLineDiff(expected, rebuilt));
            } catch (RuntimeException e) {
                report("introspect", name, "threw " + e);
            }
        }
    }

    static void report(String check, String file, String why) {
        System.out.println(why == null ? "pass " + check + " " + file : "FAIL " + check + " " + file + ": " + why);
    }

    static String gjPrint(String sdl) { return gjPrint(sdl, d -> true); }

    static String gjPrint(String sdl, java.util.function.Predicate<String> applied) {
        var registry = new graphql.schema.idl.SchemaParser().parse(sdl);
        var schema = new graphql.schema.idl.SchemaGenerator().makeExecutableSchema(registry, graphql.schema.idl.RuntimeWiring.MOCKED_WIRING);
        return new SchemaPrinter(SchemaPrinter.Options.defaultOptions().includeDirectives(applied).includeSchemaDefinition(true)).print(schema);
    }

    /**
     * Introspection as compared: built-in scalars and introspection types left out (their descriptions are the
     * implementation's own), only the spec's directives (graphql-java adds @defer and
     * @experimental_disableErrorPropagation), directive locations sorted, built-in descriptions dropped.
     */
    static JsonNode comparable(JsonNode response) {
        JsonNode s = IntrospectDiff.byName(response).path("data").path("__schema");
        var out = JSON.createObjectNode();
        var types = JSON.createObjectNode();
        s.path("types").properties().forEach(e -> { if (!e.getKey().startsWith("__") && !BUILT_IN_TYPES.contains(e.getKey())) types.set(e.getKey(), e.getValue()); });
        out.set("types", types);
        var directives = JSON.createObjectNode();
        s.path("directives").properties().forEach(e -> {
            if (!SPEC_DIRECTIVES.contains(e.getKey()) && !e.getKey().equals("defer") && !e.getKey().equals("experimental_disableErrorPropagation")) { directives.set(e.getKey(), e.getValue()); return; }
            if (!SPEC_DIRECTIVES.contains(e.getKey())) return;
            var d = ((tools.jackson.databind.node.ObjectNode) e.getValue().deepCopy());
            d.remove("description");
            d.path("args").properties().forEach(arg -> ((tools.jackson.databind.node.ObjectNode) arg.getValue()).remove("description"));
            List<String> locations = new ArrayList<>();
            d.path("locations").forEach(l -> locations.add(l.asString()));
            locations.sort(null);
            d.set("locations", JSON.valueToTree(locations));
            directives.set(e.getKey(), d);
        });
        out.set("directives", directives);
        canonical(out);
        for (String root : List.of("queryType", "mutationType", "subscriptionType", "description")) out.set(root, s.path(root));
        return out;
    }

    /**
     * Orders that carry no meaning are sorted (possibleTypes, interfaces, directive locations), and default values are
     * reprinted by one printer: graphql-java writes {text : "a"}, graphql-js and this course {text: "a"}.
     */
    static void canonical(JsonNode n) {
        if (n instanceof tools.jackson.databind.node.ObjectNode o) {
            for (String k : List.copyOf(o.propertyNames())) {
                JsonNode v = o.get(k);
                if ((k.equals("possibleTypes") || k.equals("interfaces")) && v.isArray()) {
                    List<JsonNode> items = new ArrayList<>();
                    v.forEach(items::add);
                    items.sort(java.util.Comparator.comparing(x -> x.path("name").asString("")));
                    o.set(k, JSON.valueToTree(items));
                } else if (k.equals("locations") && v.isArray()) {
                    List<String> items = new ArrayList<>();
                    v.forEach(x -> items.add(x.asString()));
                    items.sort(null);
                    o.set(k, JSON.valueToTree(items));
                } else if (k.equals("defaultValue") && v.isString()) {
                    o.put(k, graphql.language.AstPrinter.printAst(new graphql.parser.Parser().parseValue(v.asString())));
                } else if (k.equals("description") && v.isString() && v.asString().isEmpty()) {
                    o.putNull(k);   // graphql-java reports "" for a custom scalar wired without a description
                } else canonical(v);
            }
        } else if (n != null && n.isArray()) n.forEach(RoundTrip::canonical);
    }

    static String firstLineDiff(String a, String b) {
        String[] x = a.split("\n"), y = b.split("\n");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            String p = i < x.length ? x[i] : "<end>", q = i < y.length ? y[i] : "<end>";
            if (!p.equals(q)) return "line " + (i + 1) + ":\n      expected " + p + "\n      got      " + q;
        }
        return "?";
    }
}
