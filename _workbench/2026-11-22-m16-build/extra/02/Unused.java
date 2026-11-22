import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ObjectTypeExtensionDefinition;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Which fields did no client use? Reads the usage reports a registry holds and the schema, prints every field
 * coordinate (Type.field) that no operation in the window selected, one per line, sorted.
 *
 *   bin/course java Unused.java <schema.graphqls> [--reports http://localhost:4200/_reports]
 *                               [--now 2026-11-15T00:00:00Z] [--days 7]
 *
 * The window is [now - days, now). An operation counts by its own timestamp, not by when it was reported.
 */
public class Unused {
    public static void main(String[] a) throws Exception {
        Path schema = Path.of(a[0]);
        String reports = "http://localhost:4200/_reports";
        Instant now = Instant.parse("2026-11-15T00:00:00Z");
        int days = 7;
        for (int i = 1; i < a.length; i++)
            switch (a[i]) {
                case "--reports" -> reports = a[++i];
                case "--now" -> now = Instant.parse(a[++i]);
                case "--days" -> days = Integer.parseInt(a[++i]);
                default -> throw new IllegalArgumentException("unknown option " + a[i]);
            }
        long to = now.toEpochMilli(), from = to - days * 86_400_000L;

        Set<String> used = new HashSet<>();
        String body = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(reports)).build(),
                HttpResponse.BodyHandlers.ofString()).body();
        for (JsonNode report : JsonMapper.builder().build().readTree(body)) {
            JsonNode map = report.path("map");
            for (JsonNode op : report.path("operations")) {
                long t = op.path("timestamp").asLong();
                if (t < from || t >= to) continue;
                map.path(op.path("operationMapKey").asString()).path("fields").forEach(f -> used.add(f.asString()));
            }
        }

        TypeDefinitionRegistry types = new SchemaParser().parse(Files.readString(schema));
        Set<String> unused = new TreeSet<>();
        for (ObjectTypeDefinition type : types.getTypes(ObjectTypeDefinition.class))
            type.getFieldDefinitions().forEach(f -> add(unused, used, type.getName() + "." + f.getName()));
        types.objectTypeExtensions().values().forEach(list -> list.forEach((ObjectTypeExtensionDefinition type) ->
                type.getFieldDefinitions().forEach(f -> add(unused, used, type.getName() + "." + f.getName()))));
        unused.forEach(System.out::println);
    }

    static void add(Set<String> unused, Set<String> used, String coordinate) {
        if (!used.contains(coordinate)) unused.add(coordinate);
    }
}
