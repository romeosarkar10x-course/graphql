import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 12.1: a GraphQL client on java.net.http and Jackson, nothing else. The README lists what each method
 * must do; tests/Check.java calls exactly these signatures, so keep them.
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
  private final HttpClient http = HttpClient.newHttpClient();

  public ShelfieClient(String url, Path documents) {
    this.url = URI.create(url);
    this.documents = documents;
  }

  /** Sends one document; returns whatever came back, errors included. Throws only when there is no GraphQL response. */
  public Response execute(String document, Map<String, Object> variables) {
    throw new UnsupportedOperationException("TODO: POST {query, variables} as JSON and decode the response");
  }

  /** Loads documents/&lt;name&gt;.graphql and sends it with operationName = name. */
  public Response executeNamed(String name, Map<String, Object> variables) {
    throw new UnsupportedOperationException("TODO");
  }

  /** Runs documents/&lt;name&gt;.graphql and decodes data at {@code path} ("club.owner") into {@code type}. */
  public <T> T field(String name, Map<String, Object> variables, String path, Class<T> type) {
    throw new UnsupportedOperationException("TODO");
  }

  /** Like execute, but identical requests already in flight share one HTTP call and its result. */
  public CompletableFuture<Response> executeAsync(String document, Map<String, Object> variables) {
    throw new UnsupportedOperationException("TODO");
  }
}
