package dev.shelfie.harness;

import dev.shelfie.mini.MiniGraphQL;
import dev.shelfie.mini.Resolver;
import dev.shelfie.mini.Schema;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Lab 17.5: runs every conformance/*.json on your engine and compares with its "expected": data must be equal;
 * errors are compared by path and locations (messages are the implementation's own); a request error must have
 * no data. Prints "pass name" or "FAIL name: why", then "passed P of N".
 *
 *   Conformance <dir>             check your engine
 *   Conformance <dir> --write     (course authors) fill "expected" from graphql-java 26.1 and print what it gave
 */
public final class Conformance {
    static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        boolean write = a.length > 1 && a[1].equals("--write");
        String shelfie = Files.readString(Path.of("/course/reference/shelfie-ref/src/main/resources/graphql/schema.graphqls"));
        String fixture = Files.readString(dir.resolve("fixture.graphqls"));
        List<Path> tests;
        try (Stream<Path> s = Files.list(dir)) { tests = s.filter(p -> p.toString().endsWith(".json")).sorted().toList(); }
        int passed = 0;
        for (Path p : tests) {
            ObjectNode t = (ObjectNode) JSON.readTree(p.toFile());
            boolean isShelfie = t.get("schema").asString().equals("shelfie");
            String sdl = isShelfie ? shelfie : fixture;
            Map<String, Object> root = t.has("data") ? JSON.convertValue(t.get("data"), Map.class) : null;
            Map<String, Object> vars = t.has("variables") ? JSON.convertValue(t.get("variables"), Map.class) : null;
            String query = t.get("query").asString(), op = t.has("operationName") ? t.get("operationName").asString() : null;
            String name = p.getFileName().toString().replace(".json", "");
            if (write) {
                Resolver r = isShelfie ? new ShelfieResolver() : new FixtureResolver();
                JsonNode expected = JSON.valueToTree(Gj.execute(Gj.schema(sdl, r), query, op, vars, root));
                t.set("expected", expected);
                Files.writeString(p, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(t) + "\n");
                System.out.println(name + ": " + expected);
                continue;
            }
            Resolver r = isShelfie ? new ShelfieResolver() : new FixtureResolver();
            String why;
            try {
                JsonNode actual = JSON.valueToTree(MiniGraphQL.execute(new Schema(sdl), query, op, vars, r, root));
                why = compare(t.get("expected"), actual);
            } catch (RuntimeException e) {
                why = "your engine threw " + e;
            }
            if (why == null) { passed++; System.out.println("pass " + name); }
            else System.out.println("FAIL " + name + ": " + why);
        }
        if (!write) System.out.println("passed " + passed + " of " + tests.size());
    }

    static String compare(JsonNode expected, JsonNode actual) {
        if (expected.has("data") != actual.has("data"))
            return expected.has("data") ? "expected data, got none (a request error?)" : "expected a request error with no data, got data " + clip(actual.get("data"));
        if (expected.has("data") && !expected.get("data").equals(actual.get("data")))
            return "data differs:\n      expected " + clip(expected.get("data")) + "\n      got      " + clip(actual.get("data"));
        var want = errors(expected);
        var got = errors(actual);
        if (!expected.has("data")) return got.isEmpty() ? "expected a request error, got none" : null;
        if (!want.equals(got)) return "errors differ (by path and locations):\n      expected " + want + "\n      got      " + got;
        return null;
    }

    static TreeSet<String> errors(JsonNode response) {
        TreeSet<String> out = new TreeSet<>();
        if (response.has("errors"))
            for (JsonNode e : response.get("errors")) {
                StringBuilder at = new StringBuilder();
                for (JsonNode l : e.path("locations")) at.append(' ').append(l.path("line")).append(':').append(l.path("column"));
                out.add("path " + e.path("path") + " at" + (at.isEmpty() ? " (none)" : at));
            }
        return out;
    }

    static String clip(JsonNode n) { String s = String.valueOf(n); return s.length() > 300 ? s.substring(0, 300) + "..." : s; }
}
