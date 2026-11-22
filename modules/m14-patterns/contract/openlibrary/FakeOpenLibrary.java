// A fake of the two Open Library endpoints the course uses, from recorded responses, with switches to break it.
// See README.md. JDK HttpServer and Jackson (bin/course java's class path).
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public class FakeOpenLibrary {
  static final JsonMapper JSON = JsonMapper.builder().build();
  static volatile String mode = "ok";
  static volatile long delayMs = 3000;
  static final AtomicInteger requests = new AtomicInteger();
  static final List<List<String>> bibkeys = new CopyOnWriteArrayList<>();
  static final List<String> searches = new CopyOnWriteArrayList<>();
  static final List<String> userAgents = new CopyOnWriteArrayList<>();

  public static void main(String[] args) throws IOException {
    Path data = Path.of(args.length > 0 ? args[0] : "data");
    int port = args.length > 1 ? Integer.parseInt(args[1]) : 4500;
    JsonNode books = JSON.readTree(Files.readString(data.resolve("books.json")));
    Map<String, String> search = new HashMap<>();
    try (var files = Files.list(data)) {
      files.filter(p -> p.getFileName().toString().startsWith("search-")).forEach(p -> {
        String name = p.getFileName().toString();
        try { search.put(name.substring(7, name.length() - 5), Files.readString(p)); } catch (IOException e) { throw new RuntimeException(e); }
      });
    }
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 64);
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    server.createContext("/_fake/", ex -> {
      String path = ex.getRequestURI().getPath();
      Map<String, String> q = query(ex);
      switch (path) {
        case "/_fake/mode" -> { mode = q.getOrDefault("set", "ok"); delayMs = Long.parseLong(q.getOrDefault("ms", "3000")); send(ex, 200, "{\"mode\":\"" + mode + "\"}"); }
        case "/_fake/reset" -> { mode = "ok"; requests.set(0); bibkeys.clear(); searches.clear(); userAgents.clear(); send(ex, 200, "{}"); }
        case "/_fake/stats" -> send(ex, 200, JSON.writeValueAsString(Map.of("requests", requests.get(), "bibkeys", bibkeys,
            "searches", searches, "userAgents", userAgents, "mode", mode)));
        default -> send(ex, 404, "{}");
      }
    });
    server.createContext("/", ex -> {
      requests.incrementAndGet();
      userAgents.add(String.valueOf(ex.getRequestHeaders().getFirst("User-Agent")));
      Map<String, String> q = query(ex);
      String path = ex.getRequestURI().getPath();
      if (path.equals("/api/books")) bibkeys.add(List.of(q.getOrDefault("bibkeys", "").split(",")));
      if (path.equals("/search.json")) searches.add(q.getOrDefault("q", ""));
      switch (mode) {
        case "down" -> { send(ex, 503, "Service Unavailable"); return; }
        case "ratelimit" -> { ex.getResponseHeaders().set("Retry-After", "1"); send(ex, 429, "Too Many Requests"); return; }
        case "garbage" -> { ex.getResponseHeaders().set("Content-Type", "text/html"); send(ex, 200, "<html><body>Open Library is under maintenance</body></html>"); return; }
        case "slow" -> { try { Thread.sleep(delayMs); } catch (InterruptedException e) { return; } }
        default -> { }
      }
      if (path.equals("/api/books")) {
        ObjectNode out = JSON.createObjectNode();
        for (String key : q.getOrDefault("bibkeys", "").split(",")) if (books.has(key)) out.set(key, books.get(key));
        send(ex, 200, JSON.writeValueAsString(out));
      } else if (path.equals("/search.json")) {
        String term = q.getOrDefault("q", "").trim().toLowerCase(Locale.ROOT);
        send(ex, 200, search.getOrDefault(term, "{\"numFound\":0,\"start\":0,\"docs\":[]}"));
      } else send(ex, 404, "{\"error\":\"notfound\"}");
    });
    server.start();
    System.out.println("fake Open Library on :" + port + " with " + books.size() + " books and " + search.size() + " searches");
  }

  static Map<String, String> query(HttpExchange ex) {
    Map<String, String> q = new HashMap<>();
    String raw = ex.getRequestURI().getRawQuery();
    if (raw != null) for (String part : raw.split("&")) {
      int i = part.indexOf('=');
      if (i > 0) q.put(URLDecoder.decode(part.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(part.substring(i + 1), StandardCharsets.UTF_8));
    }
    return q;
  }

  static void send(HttpExchange ex, int status, String body) throws IOException {
    byte[] b = body.getBytes(StandardCharsets.UTF_8);
    if (!ex.getResponseHeaders().containsKey("Content-Type")) ex.getResponseHeaders().set("Content-Type", "application/json");
    ex.sendResponseHeaders(status, b.length);
    try (var out = ex.getResponseBody()) { out.write(b); } catch (IOException clientGone) { }
  }
}
