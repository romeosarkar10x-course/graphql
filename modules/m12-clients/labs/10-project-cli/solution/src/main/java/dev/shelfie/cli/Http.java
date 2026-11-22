package dev.shelfie.cli;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** POST a document, return the whole response ({"data", "errors"}). */
final class Http {
  static final JsonMapper JSON = JsonMapper.builder().build();
  private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();
  private final URI url;

  Http(String url) {
    this.url = URI.create(url);
  }

  JsonNode execute(String document) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("query", document);
    try {
      HttpResponse<String> r = http.send(HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(30))
          .header("Content-Type", "application/json").header("Accept", "application/graphql-response+json, application/json")
          .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
      return JSON.readTree(r.body());
    } catch (Exception e) {
      throw new IllegalStateException("cannot reach " + url + ": " + e, e);
    }
  }
}
