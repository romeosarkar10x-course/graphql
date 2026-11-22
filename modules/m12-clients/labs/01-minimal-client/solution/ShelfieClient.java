import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 12.1: a GraphQL client on java.net.http and Jackson, nothing else. Everything a client library does
 * for you is written out here: transport, documents, variables, decoding, errors and dedupe.
 */
public final class ShelfieClient {
  public record GraphQlError(String message, List<Object> path, Map<String, Object> extensions) {}

  public record Response(int status, JsonNode data, List<GraphQlError> errors) {
    public boolean hasErrors() { return !errors.isEmpty(); }
  }

  /** Thrown when a response cannot be used: transport failure, a non-2xx status, or errors where data was required. */
  public static final class GraphQlException extends RuntimeException {
    private final int status;
    private final List<GraphQlError> errors;
    private final JsonNode data;

    GraphQlException(String message, int status, List<GraphQlError> errors, JsonNode data, Throwable cause) {
      super(message, cause);
      this.status = status;
      this.errors = errors;
      this.data = data;
    }

    public int status() { return status; }
    public List<GraphQlError> errors() { return errors; }
    public JsonNode data() { return data; }
  }

  private static final ObjectMapper JSON = JsonMapper.builder().build();
  private final URI url;
  private final Path documents;
  private final HttpClient http = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();
  private final Map<String, String> loaded = new ConcurrentHashMap<>();
  /** In-flight requests by (document, variables): identical concurrent calls share one HTTP request. */
  private final Map<String, CompletableFuture<Response>> inFlight = new ConcurrentHashMap<>();

  public ShelfieClient(String url, Path documents) {
    this.url = URI.create(url);
    this.documents = documents;
  }

  /** Sends one document; returns whatever came back, errors included. Throws only when there is no GraphQL response. */
  public Response execute(String document, Map<String, Object> variables) {
    return send(document, variables, null);
  }

  /** Loads documents/&lt;name&gt;.graphql and sends it with operationName = name. */
  public Response executeNamed(String name, Map<String, Object> variables) {
    return send(document(name), variables, name);
  }

  /**
   * Runs documents/&lt;name&gt;.graphql and decodes data at {@code path} (dot-separated, e.g. "club.owner") into
   * {@code type}. Any error in the response is an exception, carrying the partial data.
   */
  public <T> T field(String name, Map<String, Object> variables, String path, Class<T> type) {
    Response r = executeNamed(name, variables);
    if (r.hasErrors()) throw new GraphQlException(r.errors().get(0).message(), r.status(), r.errors(), r.data(), null);
    JsonNode node = r.data();
    for (String step : path.split("\\.")) {
      node = node == null ? null : node.get(step);
    }
    if (node == null || node.isNull()) return null;
    return JSON.treeToValue(node, type);
  }

  /** Like execute, but identical requests already in flight share one HTTP call and its result. */
  public CompletableFuture<Response> executeAsync(String document, Map<String, Object> variables) {
    String key = document + "\u0000" + JSON.writeValueAsString(sorted(variables));
    CompletableFuture<Response> mine = new CompletableFuture<>();
    CompletableFuture<Response> existing = inFlight.putIfAbsent(key, mine);
    if (existing != null) return existing;
    http.sendAsync(request(document, variables, null), HttpResponse.BodyHandlers.ofString())
        .handle((resp, failure) -> {
          inFlight.remove(key, mine);   // later calls send again: this is dedupe, not a cache
          try {
            if (failure != null) throw transport(failure);
            mine.complete(decode(resp));
          } catch (RuntimeException e) {
            mine.completeExceptionally(e);
          }
          return null;
        });
    return mine;
  }

  private Response send(String document, Map<String, Object> variables, String operationName) {
    try {
      return decode(http.send(request(document, variables, operationName), HttpResponse.BodyHandlers.ofString()));
    } catch (IOException e) {
      throw transport(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw transport(e);
    }
  }

  private HttpRequest request(String document, Map<String, Object> variables, String operationName) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("query", document);
    if (operationName != null) body.put("operationName", operationName);
    if (variables != null && !variables.isEmpty()) body.put("variables", variables);
    return HttpRequest.newBuilder(url)
        .timeout(Duration.ofSeconds(10))
        .header("Content-Type", "application/json")
        .header("Accept", "application/graphql-response+json, application/json")
        .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
        .build();
  }

  private static Response decode(HttpResponse<String> resp) {
    int status = resp.statusCode();
    String type = resp.headers().firstValue("Content-Type").orElse("");
    JsonNode root = null;
    if (type.contains("json")) {
      try { root = JSON.readTree(resp.body()); } catch (RuntimeException e) { root = null; }
    }
    if (root == null || !root.isObject() || !(root.has("data") || root.has("errors"))) {
      throw new GraphQlException("HTTP " + status + ": not a GraphQL response", status, List.of(), null, null);
    }
    List<GraphQlError> errors = new ArrayList<>();
    for (JsonNode e : root.path("errors")) {
      List<Object> path = e.has("path") ? JSON.convertValue(e.get("path"), List.class) : null;
      Map<String, Object> ext = e.has("extensions") ? JSON.convertValue(e.get("extensions"), Map.class) : Map.of();
      errors.add(new GraphQlError(e.path("message").asString(), path, ext));
    }
    JsonNode data = root.get("data");
    if (data != null && data.isNull()) data = null;
    if (status / 100 != 2) {
      String msg = errors.isEmpty() ? "HTTP " + status : errors.get(0).message();
      throw new GraphQlException(msg, status, errors, data, null);
    }
    return new Response(status, data, List.copyOf(errors));
  }

  private String document(String name) {
    return loaded.computeIfAbsent(name, n -> {
      try {
        return Files.readString(documents.resolve(n + ".graphql"));
      } catch (IOException e) {
        throw new UncheckedIOException("no document " + n + ".graphql in " + documents, e);
      }
    });
  }

  private static GraphQlException transport(Throwable e) {
    Throwable cause = e instanceof java.util.concurrent.CompletionException && e.getCause() != null ? e.getCause() : e;
    return new GraphQlException("transport: " + cause, 0, List.of(), null, cause);
  }

  private static Object sorted(Object value) {
    if (value instanceof Map<?, ?> m) {
      Map<String, Object> out = new java.util.TreeMap<>();
      m.forEach((k, v) -> out.put(String.valueOf(k), sorted(v)));
      return out;
    }
    if (value instanceof List<?> l) return l.stream().map(ShelfieClient::sorted).toList();
    return value;
  }
}
