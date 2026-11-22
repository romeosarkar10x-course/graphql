import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public class Fail {
  public static void main(String[] a) throws Exception {
    String url = a[0]; int n = Integer.parseInt(a[1]); boolean fresh = a.length > 2;
    HttpClient shared = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(2)).build();
    int ok = 0; java.util.Map<String,Integer> why = new java.util.TreeMap<>();
    long t0 = System.nanoTime();
    for (int i = 0; i < n; i++) {
      HttpClient c = fresh ? HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(2)).build() : shared;
      try {
        var r = c.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{ hello }\"}")).build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() == 200 && r.body().contains("hello")) ok++; else why.merge("status " + r.statusCode(), 1, Integer::sum);
      } catch (Exception e) { why.merge(e.getClass().getSimpleName() + ": " + e.getMessage(), 1, Integer::sum); }
    }
    System.out.printf("%d/%d ok in %d ms %s%n", ok, n, (System.nanoTime() - t0) / 1_000_000, why);
  }
}
