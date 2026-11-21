import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Lab 9.6's measurement: the dashboard query 50 times through Varnish, three ways.
 *   bin/course java Wire.java [varnish url] [app url]
 * Prints, per mode: bytes the client sends per request (request line + body), the Varnish hit rate,
 * and the median latency. The APQ client does what Apollo's link does: hash first, text only on a miss.
 */
public class Wire {
    static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        String varnish = args.length > 0 ? args[0] : "http://varnish:6081/graphql";
        String app = args.length > 1 ? args[1] : "http://localhost:4000/graphql";
        String query = Files.readString(Path.of("/course/modules/m09-performance/contract/dashboard.graphql"))
            .lines().filter(l -> !l.startsWith("#")).reduce("", (a, b) -> a + b + "\n");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(query.getBytes(StandardCharsets.UTF_8)));
        String ext = "{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"" + hash + "\"}}";
        String body = "{\"query\":" + jsonString(query) + "}";
        String getFull = varnish + "?query=" + enc(query);
        String getHash = varnish + "?extensions=" + enc(ext);
        System.out.printf("%-10s %14s %9s %11s%n", "mode", "request bytes", "hit rate", "median ms");
        report("POST", 50, i -> send(HttpRequest.newBuilder(URI.create(varnish)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build()), ("POST /graphql " + body).length());
        report("GET", 50, i -> send(HttpRequest.newBuilder(URI.create(getFull)).GET().build()), ("GET " + getFull).length());
        report("APQ GET", 50, i -> {
            var r = send(HttpRequest.newBuilder(URI.create(getHash)).GET().build());
            if (r.body().contains("PersistedQueryNotFound")) {   // register once, by POST to the app, then retry
                send(HttpRequest.newBuilder(URI.create(app)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"query\":" + jsonString(query) + ",\"extensions\":" + ext + "}")).build());
                r = send(HttpRequest.newBuilder(URI.create(getHash)).GET().build());
            }
            return r;
        }, ("GET " + getHash).length());
    }

    interface Call { HttpResponse<String> run(int i) throws Exception; }

    static void report(String mode, int n, Call call, int bytes) throws Exception {
        int hits = 0;
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long t = System.nanoTime();
            var r = call.run(i);
            times.add(System.nanoTime() - t);
            if (r.statusCode() != 200 || !r.body().contains("\"data\"")) throw new IllegalStateException(mode + ": " + r.statusCode() + " " + r.body());
            if (r.headers().firstValue("X-Cache").orElse("").equals("HIT")) hits++;
        }
        times.sort(null);
        System.out.printf("%-10s %14d %8.0f%% %11.1f%n", mode, bytes, 100.0 * hits / n, times.get(n / 2) / 1e6);
    }

    static HttpResponse<String> send(HttpRequest r) throws Exception { return HTTP.send(r, HttpResponse.BodyHandlers.ofString()); }

    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    static String jsonString(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""; }
}
