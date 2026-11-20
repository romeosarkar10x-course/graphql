// Lab 0.4: GraphQL is an HTTP POST with a JSON body. Run: bin/course jshell first-query.jsh
// Goal: print the title of the book club "c1" is currently reading, and nothing else.
// Rules: pass "c1" as a GraphQL *variable*, not glued into the query string.
import java.net.URI;
import java.net.http.*;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

var base = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080");
var mapper = JsonMapper.builder().build();

// jshell gotcha: a line that starts with `.foo()` is NOT a continuation. Put the dot at the end of the line.
// TODO 1: build the JSON body: {"query": ..., "variables": {...}, "operationName": ...}
// TODO 2: POST it to base + "/graphql" with Content-Type: application/json
// TODO 3: read data.club.currentBook.title out of the response and print it

/exit
