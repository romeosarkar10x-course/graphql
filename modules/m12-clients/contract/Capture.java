import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Module 12's wire tap: an HTTP proxy that prints every request body on one line, then forwards it.
 *   bin/course java modules/m12-clients/contract/Capture.java 4999 http://shelfie-ref:8080/graphql
 * Point a client at http://localhost:4999/graphql to see the document, variables and operationName it sends.
 */
public class Capture {
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(args[0]);
    URI upstream = URI.create(args[1]);
    HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/", ex -> {
      byte[] body = ex.getRequestBody().readAllBytes();
      String user = ex.getRequestHeaders().getFirst("X-Shelfie-User");
      System.out.println(ex.getRequestMethod() + (user == null ? "" : " [X-Shelfie-User: " + user + "]") + " " + new String(body, StandardCharsets.UTF_8));
      System.out.flush();
      try {
        HttpRequest.Builder out = HttpRequest.newBuilder(upstream).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (user != null) out.header("X-Shelfie-User", user);
        HttpResponse<byte[]> r = http.send(out.build(), HttpResponse.BodyHandlers.ofByteArray());
        ex.getResponseHeaders().add("Content-Type", r.headers().firstValue("Content-Type").orElse("application/json"));
        ex.sendResponseHeaders(r.statusCode(), r.body().length);
        ex.getResponseBody().write(r.body());
      } catch (Exception e) {
        ex.sendResponseHeaders(502, -1);
      }
      ex.close();
    });
    server.start();
    System.err.println("capturing on :" + port + " -> " + upstream);
  }
}
