import java.net.URI;
import java.net.http.*;
public class SseLines {
  public static void main(String[] a) throws Exception {
    var http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    long t0 = System.nanoTime();
    var res = http.send(HttpRequest.newBuilder(URI.create("http://localhost:4000/graphql")).header("Content-Type", "application/json").header("Accept", "text/event-stream")
        .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"subscription { messageSent(roomId: \\\"r2\\\") { id } }\"}")).build(), HttpResponse.BodyHandlers.ofLines());
    System.out.println((System.nanoTime() - t0) / 1000000 + " headers " + res.statusCode() + " " + res.headers().map());
    res.body().forEach(l -> System.out.println((System.nanoTime() - t0) / 1000000 + " [" + l + "]"));
  }
}
