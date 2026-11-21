package dev.shelfie.secure;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 10.8: security regression tests, black box over HTTP, anonymous only.
 *   mvn test -Dshelfie.url=http://localhost:4000/graphql
 * They must pass against your hardened server and fail against the vulnerable fork.
 * TODO: at least 10 tests, one per attack or scanner finding.
 */
class SecurityRegressionTest {
  static final String URL = System.getProperty("shelfie.url", "http://localhost:4000/graphql");
  static final HttpClient HTTP = HttpClient.newHttpClient();
  static final JsonMapper JSON = JsonMapper.builder().build();

  static JsonNode post(String query) throws Exception {
    var res = HTTP.send(HttpRequest.newBuilder(URI.create(URL)).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(java.util.Map.of("query", query)))).build(),
        HttpResponse.BodyHandlers.ofString());
    return JSON.readTree(res.body());
  }

  @Test
  void introspectionIsOff() throws Exception {
    JsonNode r = post("{ __schema { types { name } } }");
    assertTrue(r.at("/data/__schema/types").isMissingNode() || r.at("/data/__schema/types").isNull(), r.toString());
  }
}
