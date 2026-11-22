// Same traffic for any router: N requests, C at a time, one query; latency percentiles, errors, and the router
// process's memory (VmRSS and its peak VmHWM from /proc) when you give its pid.
//
//   java Load.java <url> <requests> <concurrency> '<query>' [pid] [header: value]...
// prints {"requests":..,"errors":..,"p50Ms":..,"p95Ms":..,"p99Ms":..,"maxMs":..,"rssMb":..,"peakRssMb":..}
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class Load {
  public static void main(String[] args) throws Exception {
    String url = args[0];
    int n = Integer.parseInt(args[1]), c = Integer.parseInt(args[2]);
    String query = args[3];
    String pid = args.length > 4 && args[4].matches("\\d+") ? args[4] : null;
    List<String[]> headers = new ArrayList<>();
    for (int i = pid == null ? 4 : 5; i < args.length; i++) headers.add(args[i].split(":\\s*", 2));
    JsonMapper json = JsonMapper.builder().build();
    byte[] body = json.writeValueAsBytes(Map.of("query", query));
    HttpClient http = HttpClient.newBuilder().executor(Executors.newFixedThreadPool(c)).build();
    long[] ms = new long[n];
    AtomicInteger next = new AtomicInteger(), errors = new AtomicInteger();
    // warm-up: plans get cached, connections opened
    for (int i = 0; i < Math.min(20, n); i++) send(http, url, body, headers);
    try (ExecutorService pool = Executors.newFixedThreadPool(c)) {
      for (int t = 0; t < c; t++) pool.submit(() -> {
        for (int i; (i = next.getAndIncrement()) < n; ) {
          long start = System.nanoTime();
          try {
            HttpResponse<byte[]> r = send(http, url, body, headers);
            JsonNode j = json.readTree(r.body());
            if (r.statusCode() != 200 || j.has("errors")) errors.incrementAndGet();
          } catch (Exception e) { errors.incrementAndGet(); }
          ms[i] = (System.nanoTime() - start) / 1_000_000;
        }
        return null;
      });
    }
    long[] sorted = ms.clone();
    Arrays.sort(sorted);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("requests", n);
    out.put("errors", errors.get());
    out.put("p50Ms", sorted[(int) (n * 0.50)]);
    out.put("p95Ms", sorted[Math.min(n - 1, (int) (n * 0.95))]);
    out.put("p99Ms", sorted[Math.min(n - 1, (int) (n * 0.99))]);
    out.put("maxMs", sorted[n - 1]);
    if (pid != null) {
      out.put("rssMb", memory(pid, "VmRSS"));
      out.put("peakRssMb", memory(pid, "VmHWM"));
    }
    System.out.println(json.writeValueAsString(out));
    System.exit(0);
  }

  static HttpResponse<byte[]> send(HttpClient http, String url, byte[] body, List<String[]> headers) throws Exception {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofByteArray(body));
    for (String[] h : headers) b.header(h[0], h[1]);
    return http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
  }

  static long memory(String pid, String key) throws Exception {
    for (String line : Files.readAllLines(Path.of("/proc", pid, "status")))
      if (line.startsWith(key + ":")) return Long.parseLong(line.replaceAll("\\D", "")) / 1024;
    return -1;
  }
}
