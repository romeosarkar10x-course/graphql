import graphql.introspection.IntrospectionQueryBuilder;
import graphql.introspection.IntrospectionResultToSchema;
import graphql.language.Document;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 2.6: ask a server for its schema with introspection and print it as SDL.
 * Run: bin/course java SchemaDump.java http://shelfie-ref:8080/graphql > shelfie.graphqls
 *      bin/course java SchemaDump.java https://countries.trevorblades.com/ --count
 */
public class SchemaDump {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "http://shelfie-ref:8080/graphql";
        boolean countOnly = args.length > 1 && args[1].equals("--count");
        JsonMapper json = JsonMapper.builder().build();

        // TODO 1: send the standard introspection query (graphql-java builds it for you:
        //         IntrospectionQueryBuilder.build()) with introspect(...). Some public servers reject
        //         it; retry with a legacy query (see the README) when the response has errors.
        // TODO 2: turn response.data into an SDL Document (IntrospectionResultToSchema), then into a
        //         GraphQLSchema: UnExecutableSchemaGenerator.makeUnExecutableSchema(new SchemaParser().buildRegistry(doc)).
        GraphQLSchema schema = null;

        if (countOnly) {
            long types = schema.getAllTypesAsList().stream().filter(t -> !t.getName().startsWith("__")).count();
            System.out.println(types + " types");
            return;
        }
        // TODO 3: print the schema as SDL with graphql-java's SchemaPrinter.
        System.out.println("TODO");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> introspect(JsonMapper json, String url, String query) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("query", query))));
        String token = System.getenv("GITHUB_TOKEN");
        if (url.contains("api.github.com") && token != null && !token.isBlank()) builder.header("Authorization", "Bearer " + token);
        var res = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        // Some servers answer a rejected query with 400 and a GraphQL body, some with 200: read both.
        if (!res.body().trim().startsWith("{")) {
            System.err.println("HTTP " + res.statusCode() + ": " + res.body().substring(0, Math.min(200, res.body().length())));
            System.exit(2);
        }
        return json.readValue(res.body(), Map.class);
    }
}
