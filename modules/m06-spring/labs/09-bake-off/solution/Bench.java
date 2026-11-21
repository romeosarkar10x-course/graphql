import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Lab 6.9 bake-off. Starts an app, waits for its first GraphQL answer, loads it, prints one table row.
 *   java Bench.java <name> <src-dir> <workdir> -- <command to start the app on :4000...>
 * Row: | name | lines | startup ms | RSS MB | p50 ms | p95 ms | req/s |
 */
public class Bench {
    static final String QUERY = "{\"query\":\"{ club(id: \\\"c1\\\") { name memberCount owner { name } currentBook { title } } }\"}";
    static final URI URL = URI.create("http://localhost:4000/graphql");
    static final HttpClient CLIENT = HttpClient.newHttpClient();
    static final HttpRequest REQUEST = HttpRequest.newBuilder(URL).header("Content-Type", "application/json")
        .header("Accept", "application/json").POST(HttpRequest.BodyPublishers.ofString(QUERY)).build();

    public static void main(String[] args) throws Exception {
        int dash = Arrays.asList(args).indexOf("--");
        String name = args[0];
        Path src = Path.of(args[1]);
        File workdir = new File(args[2]);
        List<String> command = Arrays.asList(args).subList(dash + 1, args.length);

        long lines = lines(src);
        long t0 = System.nanoTime();
        Process app = new ProcessBuilder(command).directory(workdir).redirectErrorStream(true)
            .redirectOutput(new File("/tmp/bench-" + name.replaceAll("[^A-Za-z0-9]+", "-") + ".log")).start();
        try {
            long startupMs = waitForFirstAnswer(app, t0);
            for (int i = 0; i < 2_000; i++) send();                      // warm-up
            var latencies = Collections.synchronizedList(new ArrayList<Long>());
            int n = 4_000, threads = 8;
            long l0 = System.nanoTime();
            try (var pool = Executors.newFixedThreadPool(threads)) {
                for (int t = 0; t < threads; t++) pool.submit(() -> {
                    for (int i = 0; i < n / threads; i++) { long s = System.nanoTime(); send(); latencies.add(System.nanoTime() - s); }
                    return null;
                });
            }
            double seconds = (System.nanoTime() - l0) / 1e9;
            List<Long> sorted = latencies.stream().sorted().toList();
            System.out.printf("| %s | %d | %d | %d | %.2f | %.2f | %.0f |%n", name, lines, startupMs, rssMb(app.pid()),
                percentile(sorted, 50), percentile(sorted, 95), n / seconds);
        } finally {
            app.destroy();
            app.waitFor();
        }
    }

    static long waitForFirstAnswer(Process app, long t0) throws Exception {
        while (System.nanoTime() - t0 < 120_000_000_000L) {
            if (!app.isAlive()) throw new IllegalStateException("the app exited; see /tmp/bench-*.log");
            try {
                var r = CLIENT.send(REQUEST, HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 200 && r.body().contains("Midnight")) return (System.nanoTime() - t0) / 1_000_000;
            } catch (java.io.IOException notYet) { /* not listening yet */ }
            Thread.sleep(10);
        }
        throw new IllegalStateException("no answer within 120 s");
    }

    static void send() throws Exception {
        var r = CLIENT.send(REQUEST, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IllegalStateException("HTTP " + r.statusCode());
    }

    static double percentile(List<Long> sortedNanos, int p) {
        return sortedNanos.get((int) Math.ceil(p / 100.0 * sortedNanos.size()) - 1) / 1e6;
    }

    /** Resident memory of the app's process (Linux): VmRSS in /proc. */
    static long rssMb(long pid) throws Exception {
        return Files.readAllLines(Path.of("/proc/" + pid + "/status")).stream().filter(l -> l.startsWith("VmRSS:"))
            .map(l -> Long.parseLong(l.replaceAll("\\D", "")) / 1024).findFirst().orElse(-1L);
    }

    /** Non-blank lines of Java written by hand: Data.java and generated code are not counted. */
    static long lines(Path src) throws Exception {
        try (Stream<Path> files = Files.walk(src)) {
            return files.filter(f -> f.toString().endsWith(".java") && !f.getFileName().toString().equals("Data.java"))
                .flatMap(f -> { try { return Files.readAllLines(f).stream(); } catch (Exception e) { throw new RuntimeException(e); } })
                .filter(l -> !l.isBlank()).count();
        }
    }
}
