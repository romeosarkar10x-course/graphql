package dev.shelfie.secure;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 10.8: security regression tests, black box over HTTP, anonymous only.
 *   mvn test -Dshelfie.url=http://localhost:4000/graphql
 * Each test is one attack from lab 10.3 or one graphql-cop finding. They pass against the hardened server
 * and fail against the vulnerable fork.
 */
class SecurityRegressionTest {
  static final String URL = System.getProperty("shelfie.url", "http://localhost:4000/graphql");
  static final HttpClient HTTP = HttpClient.newHttpClient();
  static final JsonMapper JSON = JsonMapper.builder().build();

  static HttpResponse<String> send(String contentType, String body, String... headers) throws Exception {
    var b = HttpRequest.newBuilder(URI.create(URL)).header("Content-Type", contentType).POST(HttpRequest.BodyPublishers.ofString(body));
    for (int i = 0; i < headers.length; i += 2) b.header(headers[i], headers[i + 1]);
    return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
  }

  static JsonNode post(String query, String... headers) throws Exception {
    return JSON.readTree(send("application/json", JSON.writeValueAsString(Map.of("query", query)), headers).body());
  }

  static boolean refused(JsonNode r) {
    return r.has("errors") && (r.path("data").isMissingNode() || r.path("data").isNull());
  }

  static String repeat(String part, int n) {
    var sb = new StringBuilder("{ ");
    for (int i = 0; i < n; i++) sb.append(part.replace("#", String.valueOf(i))).append(' ');
    return sb.append('}').toString();
  }

  @Test
  void introspectionIsOff() throws Exception {
    JsonNode r = post("{ __schema { types { name fields { name } } } }");
    assertTrue(r.at("/data/__schema/types").isMissingNode() || r.at("/data/__schema/types").isNull(), r.toString());
  }

  @Test
  void aliasOverloadingIsRefused() throws Exception {
    assertTrue(refused(post(repeat("a#: __typename", 101))));
  }

  @Test
  void fieldDuplicationIsRefused() throws Exception {
    assertTrue(refused(post(repeat("__typename", 500))));
  }

  @Test
  void directiveOverloadingGetsOneError() throws Exception {
    JsonNode r = post("query cop { __typename @aa@aa@aa@aa@aa@aa@aa@aa@aa@aa }");
    assertEquals(1, r.path("errors").size(), r.toString());
  }

  @Test
  void deepNestingIsRefused() throws Exception {
    String q = "{ book(id: \"b1\") { " + "reviews(first: 1) { book { ".repeat(6) + "title" + " } }".repeat(6) + " } }";
    assertTrue(refused(post(q)));
  }

  @Test
  void fanOutIsRefused() throws Exception {
    assertTrue(refused(post("{ clubs(first: 12) { members(first: 10) { clubs(first: 12) { members(first: 10) { clubs(first: 12) { name } } } } } }")));
  }

  @Test
  void hugePagesAreRefused() throws Exception {
    JsonNode r = post("{ books(first: 1000) { id } }");
    assertTrue(r.has("errors") && r.at("/data/books").isMissingNode() || r.at("/data/books").isNull(), r.toString());
  }

  @Test
  void privateReviewsDoNotLeakThroughNode() throws Exception {
    JsonNode r = post("{ node(id: \"v61\") { id } }");
    assertTrue(r.at("/data/node").isNull() || r.at("/data/node").isMissingNode(), r.toString());
  }

  @Test
  void textPlainIsNotGraphQL() throws Exception {
    var res = send("text/plain", "{\"query\": \"{ __typename }\"}");
    assertEquals(415, res.statusCode(), res.body());
  }

  @Test
  void mutationsOverGetAreRefused() throws Exception {
    var res = HTTP.send(HttpRequest.newBuilder(URI.create(URL + "?query=" + java.net.URLEncoder.encode("mutation { removeReview(id: \"v31\") }",
        java.nio.charset.StandardCharsets.UTF_8))).GET().build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(405, res.statusCode());
  }

  @Test
  void unsignedTokensAreRejected() throws Exception {
    var b64 = Base64.getUrlEncoder().withoutPadding();
    String token = b64.encodeToString("{\"alg\":\"none\"}".getBytes()) + "."
        + b64.encodeToString("{\"preferred_username\":\"root\",\"roles\":[\"admin\"]}".getBytes()) + ".";
    var res = send("application/json", "{\"query\": \"{ __typename }\"}", "Authorization", "Bearer " + token);
    assertEquals(401, res.statusCode());
  }

  @Test
  void internalErrorsAreMasked() throws Exception {
    JsonNode r = post("{ book(id: \"b1\") { recommendations(first: 11) { id } } }");
    assertEquals("INTERNAL_ERROR", r.at("/errors/0/extensions/classification").asString(), r.toString());
  }

  @Test
  void socketsFromOtherOriginsAreRefused() {
    var ws = HTTP.newWebSocketBuilder().header("Origin", "http://evil.test").subprotocols("graphql-transport-ws")
        .buildAsync(URI.create(URL.replace("http://", "ws://")), new WebSocket.Listener() {});
    assertThrows(Exception.class, () -> ws.get(5, TimeUnit.SECONDS).abort());
  }
}
