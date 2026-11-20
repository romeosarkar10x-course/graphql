// Lab 0.4: GraphQL is an HTTP POST with a JSON body. Run: bin/course jshell first-query.jsh
import java.net.URI;
import java.net.http.*;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

var base = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080");
var mapper = JsonMapper.builder().build();

var body = mapper.writeValueAsString(Map.of(
    "query", "query CurrentBook($clubId: ID!) { club(id: $clubId) { currentBook { title } } }",
    "variables", Map.of("clubId", "c1"),
    "operationName", "CurrentBook"));

// jshell ends a statement at a complete line, so chained calls keep the dot at the END of a line.
var request = HttpRequest.newBuilder(URI.create(base + "/graphql")).
    header("Content-Type", "application/json").
    header("Accept", "application/graphql-response+json").
    POST(HttpRequest.BodyPublishers.ofString(body)).
    build();

var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
JsonNode json = mapper.readTree(response.body());
System.out.println(json.at("/data/club/currentBook/title").asString());
/exit
