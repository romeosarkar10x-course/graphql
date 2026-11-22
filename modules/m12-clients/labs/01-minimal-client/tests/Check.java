import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Lab 12.1's checks. Run next to the learner's ShelfieClient.java: java Check.java <ref graphql url> <documents dir>. */
public class Check {
  public record Book(String id, String title, List<String> authors, Integer publishedYear) {}
  public record User(String id, String name, String handle) {}

  static final JsonMapper JSON = JsonMapper.builder().build();
  static int fails = 0;
  static final List<JsonNode> seen = new CopyOnWriteArrayList<>();
  static final List<Map<String, String>> headers = new CopyOnWriteArrayList<>();
  static final AtomicReference<Reply> reply = new AtomicReference<>();
  record Reply(int status, String type, String body, long delayMs) {}

  static void check(String name, boolean ok, String detail) {
    System.out.println((ok ? "PASS " : "FAIL ") + name + (ok || detail == null ? "" : "\t" + detail));
    if (!ok) fails++;
  }

  interface Step { void run() throws Exception; }
  static void attempt(String name, Step s) {
    try { s.run(); } catch (Throwable t) { check(name, false, t.toString()); }
  }

  public static void main(String[] a) throws Exception {
    String ref = a[0];
    Path docs = Path.of(a[1]);
    HttpServer stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    stub.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
    stub.createContext("/graphql", ex -> {
      String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      try { seen.add(JSON.readTree(body)); } catch (RuntimeException e) { seen.add(JSON.getNodeFactory().textNode(body)); }
      headers.add(Map.of("method", ex.getRequestMethod(),
          "content-type", String.valueOf(ex.getRequestHeaders().getFirst("Content-Type")),
          "accept", String.valueOf(ex.getRequestHeaders().getFirst("Accept"))));
      Reply r = reply.get();
      try { Thread.sleep(r.delayMs()); } catch (InterruptedException ignored) {}
      byte[] out = r.body().getBytes(StandardCharsets.UTF_8);
      ex.getResponseHeaders().add("Content-Type", r.type());
      ex.sendResponseHeaders(r.status(), out.length);
      ex.getResponseBody().write(out);
      ex.close();
    });
    stub.start();
    String stubUrl = "http://127.0.0.1:" + stub.getAddress().getPort() + "/graphql";
    String gqlJson = "application/graphql-response+json; charset=utf-8";
    ShelfieClient s = new ShelfieClient(stubUrl, docs);

    // 1. The wire format of a named document.
    attempt("wire: POST, JSON body with query, operationName and variables", () -> {
      seen.clear(); headers.clear();
      reply.set(new Reply(200, gqlJson, "{\"data\":{\"book\":null}}", 0));
      s.executeNamed("BookById", Map.of("id", "b1"));
      JsonNode b = seen.get(0);
      Map<String, String> h = headers.get(0);
      boolean ok = h.get("method").equals("POST") && h.get("content-type").startsWith("application/json")
          && b.path("query").asString().contains("query BookById") && b.path("operationName").asString().equals("BookById")
          && b.path("variables").path("id").asString().equals("b1");
      check("wire: POST, JSON body with query, operationName and variables", ok, "method " + h.get("method") + ", content-type " + h.get("content-type") + ", body " + b);
      check("wire: Accept asks for application/graphql-response+json", h.get("accept").contains("application/graphql-response+json"), "Accept: " + h.get("accept"));
    });

    // 2. Variables are JSON values, not strings pasted into the document.
    attempt("variables: nested input and non-ASCII text arrive as JSON", () -> {
      seen.clear();
      reply.set(new Reply(200, gqlJson, "{\"data\":{\"createReview\":null}}", 0));
      Map<String, Object> input = Map.of("bookId", "b3", "rating", 5, "body", "Mémoire « vive » \"quoted\" 東京");
      s.execute("mutation($input: CreateReviewInput!) { createReview(input: $input) { review { id } } }", Map.of("input", input));
      JsonNode v = seen.get(0).path("variables").path("input");
      boolean ok = v.path("rating").isInt() && v.path("rating").asInt() == 5 && v.path("body").asString().equals(input.get("body"))
          && !seen.get(0).path("query").asString().contains("Mémoire");
      check("variables: nested input and non-ASCII text arrive as JSON", ok, "variables were " + seen.get(0).path("variables"));
    });

    // 3. Errors next to partial data.
    attempt("errors: execute returns errors and partial data without throwing", () -> {
      reply.set(new Reply(200, gqlJson, "{\"errors\":[{\"message\":\"recommender down\",\"path\":[\"book\",\"title\"],\"extensions\":{\"classification\":\"INTERNAL_ERROR\"}}],\"data\":{\"book\":{\"id\":\"b1\",\"title\":null,\"authors\":[],\"publishedYear\":null}}}", 0));
      ShelfieClient.Response r = s.executeNamed("BookById", Map.of("id", "b1"));
      boolean ok = r.status() == 200 && r.errors().size() == 1 && r.errors().get(0).message().equals("recommender down")
          && List.of("book", "title").equals(r.errors().get(0).path()) && "INTERNAL_ERROR".equals(r.errors().get(0).extensions().get("classification"))
          && r.data().path("book").path("id").asString().equals("b1");
      check("errors: execute returns errors and partial data without throwing", ok, "got " + r);
      try {
        s.field("BookById", Map.of("id", "b1"), "book", Book.class);
        check("errors: field() throws GraphQlException carrying the errors and the partial data", false, "nothing was thrown");
      } catch (ShelfieClient.GraphQlException e) {
        check("errors: field() throws GraphQlException carrying the errors and the partial data",
            e.errors().size() == 1 && e.data() != null && e.data().path("book").path("id").asString().equals("b1"), "errors " + e.errors() + ", data " + e.data());
      }
    });

    // 4. HTTP statuses.
    attempt("status: a 400 with a GraphQL body becomes GraphQlException(status 400, its error)", () -> {
      reply.set(new Reply(400, gqlJson, "{\"errors\":[{\"message\":\"Syntax error at line 1\"}]}", 0));
      try {
        s.execute("{ book(", Map.of());
        check("status: a 400 with a GraphQL body becomes GraphQlException(status 400, its error)", false, "nothing was thrown");
      } catch (ShelfieClient.GraphQlException e) {
        check("status: a 400 with a GraphQL body becomes GraphQlException(status 400, its error)",
            e.status() == 400 && e.errors().size() == 1 && e.errors().get(0).message().startsWith("Syntax error"), "status " + e.status() + ", errors " + e.errors());
      }
    });
    attempt("status: a 502 HTML page becomes GraphQlException(status 502), not a JSON parse error", () -> {
      reply.set(new Reply(502, "text/html", "<html><body>Bad Gateway</body></html>", 0));
      try {
        s.execute("{ hello }", Map.of());
        check("status: a 502 HTML page becomes GraphQlException(status 502), not a JSON parse error", false, "nothing was thrown");
      } catch (ShelfieClient.GraphQlException e) {
        check("status: a 502 HTML page becomes GraphQlException(status 502), not a JSON parse error", e.status() == 502, "status " + e.status() + ": " + e.getMessage());
      }
    });

    // 5. Dedupe of identical in-flight requests.
    attempt("dedupe: 10 identical concurrent calls send 1 request, and all 10 get the answer", () -> {
      seen.clear();
      reply.set(new Reply(200, gqlJson, "{\"data\":{\"hello\":\"Hello from Shelfie\"}}", 400));
      List<CompletableFuture<ShelfieClient.Response>> calls = new ArrayList<>();
      for (int i = 0; i < 10; i++) calls.add(s.executeAsync("{ hello }", Map.of("x", 1, "y", List.of(1, 2))));
      List<String> answers = new ArrayList<>();
      for (var c : calls) answers.add(c.get(5, TimeUnit.SECONDS).data().path("hello").asString());
      check("dedupe: 10 identical concurrent calls send 1 request, and all 10 get the answer",
          seen.size() == 1 && answers.stream().allMatch("Hello from Shelfie"::equals), seen.size() + " requests, answers " + answers);
      seen.clear();
      var x = s.executeAsync("{ hello }", Map.of("x", 1));
      var y = s.executeAsync("{ hello }", Map.of("x", 2));
      var z = s.executeAsync("{ hello }", new java.util.LinkedHashMap<>(Map.of("y", 2, "x", 1)));
      var w = s.executeAsync("{ hello }", new java.util.TreeMap<>(Map.of("x", 1, "y", 2)));
      CompletableFuture.allOf(x, y, z, w).get(5, TimeUnit.SECONDS);
      check("dedupe: different variables send separate requests; the same variables in another order do not", seen.size() == 3, seen.size() + " requests for {x:1}, {x:2}, {y:2,x:1}, {x:1,y:2} (want 3)");
      seen.clear();
      s.executeAsync("{ hello }", Map.of("x", 1)).get(5, TimeUnit.SECONDS);
      check("dedupe: a call after the first finished sends again (dedupe is not a cache)", seen.size() == 1, seen.size() + " requests");
    });

    attempt("dedupe: a transport failure reaches every waiting caller, and the next call tries again", () -> {
      ShelfieClient dead = new ShelfieClient("http://127.0.0.1:9/graphql", docs);
      var f1 = dead.executeAsync("{ hello }", Map.of());
      var f2 = dead.executeAsync("{ hello }", Map.of());
      int failed = 0;
      for (var f : List.of(f1, f2)) {
        try { f.get(10, TimeUnit.SECONDS); } catch (java.util.concurrent.ExecutionException e) { if (e.getCause() instanceof ShelfieClient.GraphQlException) failed++; }
      }
      boolean retried = false;
      try { dead.executeAsync("{ hello }", Map.of()).get(10, TimeUnit.SECONDS); } catch (java.util.concurrent.ExecutionException e) { retried = e.getCause() instanceof ShelfieClient.GraphQlException; }
      check("dedupe: a transport failure reaches every waiting caller, and the next call tries again", failed == 2 && retried, failed + " of 2 callers got a GraphQlException; third call " + (retried ? "failed again" : "did not fail with GraphQlException"));
    });
    stub.stop(0);

    // 6. The real server: typed decoding, nested paths, request counting.
    ShelfieClient real = new ShelfieClient(ref, docs);
    attempt("decode: field(BookById, b1, \"book\", Book.class) against Shelfie", () -> {
      Book b = real.field("BookById", Map.of("id", "b1"), "book", Book.class);
      check("decode: field(BookById, b1, \"book\", Book.class) against Shelfie",
          b != null && b.title().equals("The Left Hand of Darkness") && b.authors().equals(List.of("Ursula K. Le Guin")) && b.publishedYear() == 1969, "got " + b);
      check("decode: a null field decodes to null", real.field("BookById", Map.of("id", "nope"), "book", Book.class) == null, "book nope was not null");
    });
    attempt("decode: a nested path (club.owner) decodes to User", () -> {
      User u = real.field("ClubOwner", Map.of("id", "c1"), "club.owner", User.class);
      check("decode: a nested path (club.owner) decodes to User", u != null && u.name().equals("Kemi Adeyemi"), "got " + u);
    });
    attempt("errors: a validation error from Shelfie is a GraphQlException from field()", () -> {
      try {
        ShelfieClient.Response r = real.execute("{ book(id: \"b1\") { nope } }", Map.of());
        check("errors: a validation error from Shelfie comes back in errors", r.hasErrors() && r.errors().get(0).message().contains("nope"), "got " + r);
      } catch (ShelfieClient.GraphQlException e) {
        check("errors: a validation error from Shelfie comes back in errors", e.errors().size() > 0, "thrown: " + e.getMessage() + " " + e.errors());
      }
    });
    attempt("dedupe: 20 concurrent identical calls reach Shelfie once", () -> {
      HttpClient h = HttpClient.newHttpClient();
      String base = ref.replace("/graphql", "");
      h.send(HttpRequest.newBuilder(URI.create(base + "/_course/reset")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
      List<CompletableFuture<ShelfieClient.Response>> calls = new ArrayList<>();
      for (int i = 0; i < 20; i++) calls.add(real.executeAsync("{ books(first: 10) { title reviews(last: 5) { body author { name } } } }", Map.of()));
      for (var c : calls) c.get(10, TimeUnit.SECONDS);
      JsonNode stats = JSON.readTree(h.send(HttpRequest.newBuilder(URI.create(base + "/_course/stats")).build(), HttpResponse.BodyHandlers.ofString()).body());
      check("dedupe: 20 concurrent identical calls reach Shelfie once", stats.path("graphql").asInt() == 1, "Shelfie counted " + stats.path("graphql").asInt() + " GraphQL requests");
    });
    System.exit(fails == 0 ? 0 : 1);
  }
}
