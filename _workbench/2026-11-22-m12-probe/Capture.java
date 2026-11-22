import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;

/** Logging proxy: prints every request body, forwards it to the URL in args[1]. */
public class Capture {
  public static void main(String[] a) throws Exception {
    var http = HttpClient.newHttpClient();
    var s = HttpServer.create(new InetSocketAddress(Integer.parseInt(a[0])), 0);
    s.createContext("/", ex -> {
      byte[] body = ex.getRequestBody().readAllBytes();
      System.out.println(">>> " + ex.getRequestMethod() + " " + new String(body));
      try {
        var r = http.send(HttpRequest.newBuilder(URI.create(a[1])).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(), HttpResponse.BodyHandlers.ofByteArray());
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(r.statusCode(), r.body().length); ex.getResponseBody().write(r.body());
      } catch (Exception e) { ex.sendResponseHeaders(502, -1); }
      ex.close();
    });
    s.start();
  }
}
