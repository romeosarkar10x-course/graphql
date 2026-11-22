import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Open-model load for Module 16.5-16.6: --rate requests a second arrive on schedule for --seconds, whether or not
 * earlier ones have finished (users do not wait for each other), so a slow server cannot hide its latency by
 * slowing the load down. Latency counts from when a request was due.
 *
 *   bin/course java modules/m16-production/contract/Peak.java [--url http://localhost:4000/graphql]
 *        [--seconds 12] [--rate 100] [--body file.json] [--timeout-ms 5000]
 *
 * The default body is the club page (contract/club-page.json). Prints one JSON line:
 *   {"requests", "p50", "p95", "p99", "max" (ms), "failed" (no 200, timeout, or no "name" in the answer),
 *    "degraded" (answers with a REVIEWS_UNAVAILABLE error), "p95PerSecond": [p95 of the requests due in each second]}
 */
public class Peak {
    public static void main(String[] a) throws Exception {
        String url = "http://localhost:4000/graphql";
        int seconds = 12, rate = 100;
        long timeoutMs = 5000;
        Path bodyFile = Path.of(System.getProperty("peak.dir", "/course/modules/m16-production/contract"), "club-page.json");
        for (int i = 0; i < a.length; i++)
            switch (a[i]) {
                case "--url" -> url = a[++i];
                case "--seconds" -> seconds = Integer.parseInt(a[++i]);
                case "--rate" -> rate = Integer.parseInt(a[++i]);
                case "--body" -> bodyFile = Path.of(a[++i]);
                case "--timeout-ms" -> timeoutMs = Long.parseLong(a[++i]);
                default -> throw new IllegalArgumentException("unknown option " + a[i]);
            }
        String body = Files.readString(bodyFile);
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
                .header("graphql-client-name", "web").timeout(Duration.ofMillis(timeoutMs))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        List<List<Long>> bySecond = new ArrayList<>();
        for (int s = 0; s < seconds; s++) bySecond.add(Collections.synchronizedList(new ArrayList<>()));
        AtomicInteger failed = new AtomicInteger(), degraded = new AtomicInteger();
        long start = System.nanoTime(), gap = 1_000_000_000L / rate;
        List<Thread> requests = new ArrayList<>();
        for (long n = 0; n < (long) rate * seconds; n++) {
            long due = start + n * gap, wait = due - System.nanoTime();
            if (wait > 0) Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
            List<Long> bucket = bySecond.get((int) (n / rate));
            requests.add(Thread.ofVirtual().start(() -> {
                try {
                    HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
                    if (r.statusCode() != 200 || !r.body().contains("\"name\"")) failed.incrementAndGet();
                    if (r.body().contains("REVIEWS_UNAVAILABLE")) degraded.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                }
                bucket.add((System.nanoTime() - due) / 1_000_000);
            }));
        }
        for (Thread t : requests) t.join();
        List<Long> sorted = new ArrayList<>();
        StringBuilder ps = new StringBuilder();
        for (int s = 0; s < seconds; s++) {
            List<Long> b = new ArrayList<>(bySecond.get(s));
            Collections.sort(b);
            sorted.addAll(b);
            ps.append(s == 0 ? "" : ",").append(b.isEmpty() ? -1 : pct(b, 95));
        }
        Collections.sort(sorted);
        System.out.printf("{\"requests\":%d,\"p50\":%d,\"p95\":%d,\"p99\":%d,\"max\":%d,\"failed\":%d,\"degraded\":%d,\"p95PerSecond\":[%s]}%n",
                sorted.size(), pct(sorted, 50), pct(sorted, 95), pct(sorted, 99), sorted.getLast(), failed.get(), degraded.get(), ps);
    }

    static long pct(List<Long> sorted, int p) {
        return sorted.get(Math.min(sorted.size() - 1, (int) Math.ceil(p / 100.0 * sorted.size()) - 1));
    }
}
