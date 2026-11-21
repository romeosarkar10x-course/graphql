import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/** Lab 6.1 load test: n concurrent slowHello requests. java Load.java [n=1000] [url=http://localhost:4000/graphql] */
public class Load {
    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
        var url = URI.create(args.length > 1 ? args[1] : "http://localhost:4000/graphql");
        var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        var request = HttpRequest.newBuilder(url).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{ slowHello servedBy }\"}")).build();
        for (int round = 0; round < 2; round++) {   // round 0 warms up connections and the JIT
            long t0 = System.nanoTime();
            var all = new ArrayList<CompletableFuture<HttpResponse<String>>>();
            for (int i = 0; i < n; i++) all.add(client.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
            // A burst of new connections can overflow the server's accept queue: count resets, do not crash.
            long ok = all.stream().map(f -> f.handle((r, e) -> r).join()).filter(r -> r != null && r.statusCode() == 200).count();
            double s = (System.nanoTime() - t0) / 1e9;
            if (round == 1) System.out.printf("%d requests (%d ok) in %.0f ms: %.0f req/s, served by %s threads%n",
                n, ok, s * 1000, n / s, all.stream().map(f -> f.handle((r, e) -> r).join()).filter(r -> r != null)
                    .findFirst().map(r -> r.body().contains("virtual") ? "virtual" : "platform").orElse("no"));
        }
    }
}
