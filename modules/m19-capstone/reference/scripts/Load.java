// Toolshed's load test (docs/LOAD.md): a mix of the catalogue, a tool page and "my loans" through the router,
// with persisted ids, on virtual threads. JDK only.
//   java scripts/Load.java [--seconds 20] [--concurrency 8] [--api http://localhost:4000/graphql] [--token T]
// Prints requests, errors, throughput and p50/p95/p99 per operation, and the SQL statements per request (from
// both subgraphs' toolshed.db.statements counters).
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Load {
    record Op(String name, String body, int weight, boolean signedIn) {}

    public static void main(String[] a) throws Exception {
        Map<String, String> o = new HashMap<>(Map.of("seconds", "20", "concurrency", "8", "api", "http://localhost:4000/graphql", "token", ""));
        for (int i = 0; i + 1 < a.length; i += 2) o.put(a[i].substring(2), a[i + 1]);
        Map<String, String> ids = new HashMap<>();
        for (String line : Files.readAllLines(Path.of("tests/ids.env"))) { String[] kv = line.split("=", 2); ids.put(kv[0], kv[1]); }
        List<Op> mix = List.of(
                new Op("ToolPage", "{\"documentId\":\"" + ids.get("ToolPage") + "\",\"variables\":{\"first\":12}}", 6, false),
                new Op("ToolPage+filter", "{\"documentId\":\"" + ids.get("ToolPage") + "\",\"variables\":{\"first\":12,\"filter\":{\"powered\":true},\"sort\":\"NEWEST\"}}", 2, false),
                new Op("ToolDetail", "{\"documentId\":\"" + ids.get("ToolDetail") + "\",\"variables\":{\"by\":{\"barcode\":\"TS-0005\"}}}", 1, false),
                new Op("MyLoans", "{\"documentId\":\"" + ids.get("MyLoans") + "\"}", 1, true));
        List<Op> wheel = new ArrayList<>();
        for (Op op : mix) for (int i = 0; i < op.weight(); i++) wheel.add(op);
        HttpClient http = HttpClient.newBuilder().executor(Executors.newVirtualThreadPerTaskExecutor()).build();
        URI api = URI.create(o.get("api"));
        String token = o.get("token");
        Map<String, ConcurrentLinkedQueue<Long>> times = new ConcurrentHashMap<>();
        AtomicInteger errors = new AtomicInteger(), done = new AtomicInteger();
        double statementsBefore = statements();
        long end = System.nanoTime() + Long.parseLong(o.get("seconds")) * 1_000_000_000L;
        int c = Integer.parseInt(o.get("concurrency"));
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int w = 0; w < c; w++) {
                int seed = w;
                pool.submit(() -> {
                    Random r = new Random(seed);
                    while (System.nanoTime() < end) {
                        Op op = wheel.get(r.nextInt(wheel.size()));
                        HttpRequest.Builder req = HttpRequest.newBuilder(api).header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(op.body()));
                        if (op.signedIn() && !token.isEmpty()) req.header("Authorization", "Bearer " + token);
                        long t0 = System.nanoTime();
                        try {
                            HttpResponse<String> res = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
                            if (res.statusCode() != 200 || res.body().contains("\"errors\"")) errors.incrementAndGet();
                        } catch (Exception e) {
                            errors.incrementAndGet();
                        }
                        times.computeIfAbsent(op.name(), k -> new ConcurrentLinkedQueue<>()).add(System.nanoTime() - t0);
                        done.incrementAndGet();
                    }
                    return null;
                });
            }
        }
        double statements = statements() - statementsBefore;
        System.out.printf("%d requests in %s s at concurrency %d: %.0f/s, %d errors, %.2f SQL statements per request%n",
                done.get(), o.get("seconds"), c, done.get() / Double.parseDouble(o.get("seconds")), errors.get(), statements / done.get());
        System.out.printf("%-16s %8s %8s %8s %8s%n", "operation", "count", "p50 ms", "p95 ms", "p99 ms");
        for (var e : new TreeMap<>(times).entrySet()) {
            long[] t = e.getValue().stream().mapToLong(Long::longValue).sorted().toArray();
            System.out.printf("%-16s %8d %8.1f %8.1f %8.1f%n", e.getKey(), t.length, t[t.length / 2] / 1e6, t[(int) (t.length * 0.95)] / 1e6, t[(int) (t.length * 0.99)] / 1e6);
        }
    }

    static double statements() throws Exception {
        double sum = 0;
        HttpClient http = HttpClient.newHttpClient();
        for (int port : new int[] {4001, 4002}) {
            String body = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/metrics/toolshed.db.statements")).build(),
                    HttpResponse.BodyHandlers.ofString()).body();
            int i = body.indexOf("\"value\":");
            sum += Double.parseDouble(body.substring(i + 8, body.indexOf('}', i)).trim());
        }
        return sum;
    }
}
