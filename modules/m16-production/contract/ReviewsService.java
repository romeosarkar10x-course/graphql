import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shelfie's reviews service (Module 16): the dependency Shelfie calls for every Book.reviews. Plain HTTP + JSON.
 *
 *   bin/course java modules/m16-production/contract/ReviewsService.java [port]      (default 4100)
 *
 *   GET  /reviews?books=b1,b2         {"b1": [review, ...], "b2": []}, newest first
 *   POST /reviews                     {"bookId","authorId","rating","body"} -> the stored review (with its seq)
 *   GET  /events?after=N&waitMs=M     reviews with seq > N, oldest first; waits up to M ms for one (long poll)
 *   POST /_fault?mode=ok|slow|error&ms=N   ok; slow: every /reviews call sleeps N ms (default 2000); error: 503
 *   GET  /_stats                      {"reviewCalls": N, "events": N, "mode": "ok"}      POST /_stats/reset
 *
 * A review: {"seq": 1, "id": "r1", "bookId": "b1", "authorId": "u1", "rating": 5, "body": "..."}. Seed: r1..r15,
 * the reference server's. "Down" is not a mode: stop the process (that is what an outage looks like to Shelfie).
 */
public class ReviewsService {
    record Review(int seq, String id, String bookId, String authorId, int rating, String body) {}

    static final List<Review> REVIEWS = new ArrayList<>();
    static final AtomicLong CALLS = new AtomicLong(), EVENTS = new AtomicLong();
    static volatile String mode = "ok";
    static volatile long slowMs = 2000;
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final Object LOCK = new Object();

    static {
        String[][] seed = {
            {"b1", "u1", "5", "Changed how I think about gender in fiction."},
            {"b1", "u2", "4", "Slow first half, devastating second."},
            {"b1", "u3", "5", "The ice crossing chapter. That's it, that's the review."},
            {"b1", "u10", "3", "Beautiful but I wanted more physics."},
            {"b1", "u8", "4", "Estraven deserved better."},
            {"b2", "u1", "5", "A house with infinite halls and one perfect narrator."},
            {"b2", "u6", "4", "Listened twice in a row."},
            {"b4", "u7", "5", "Labyrinthine in every sense."},
            {"b4", "u12", "4", "Read every Latin footnote. Worth it."},
            {"b6", "u10", "5", "Finally, physics."},
            {"b6", "u4", "4", "Rocky is the best character of the decade."},
            {"b9", "u5", "5", "Funny and furious."},
            {"b9", "u3", "4", "The afterlife as bureaucracy, brilliant."},
            {"b3", "u9", "5", "Quietly crushing."},
            {"b8", "u1", "4", "Survival is insufficient."}};
        for (String[] s : seed) add(s[0], s[1], Integer.parseInt(s[2]), s[3]);
    }

    static Review add(String bookId, String authorId, int rating, String body) {
        synchronized (LOCK) {
            int seq = REVIEWS.size() + 1;
            Review r = new Review(seq, "r" + seq, bookId, authorId, rating, body);
            REVIEWS.add(r);
            LOCK.notifyAll();
            return r;
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 4100;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 512);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/reviews", ex -> handle(ex, () -> {
            if (ex.getRequestMethod().equals("POST")) {
                Map<?, ?> in = JSON.readValue(ex.getRequestBody(), Map.class);
                return add((String) in.get("bookId"), (String) in.get("authorId"),
                        ((Number) in.get("rating")).intValue(), (String) in.get("body"));
            }
            CALLS.incrementAndGet();
            if (mode.equals("slow")) Thread.sleep(slowMs);
            if (mode.equals("error")) return 503;
            Map<String, List<Review>> out = new LinkedHashMap<>();
            for (String id : query(ex).getOrDefault("books", "").split(",")) {
                if (id.isBlank()) continue;
                synchronized (LOCK) {
                    out.put(id, REVIEWS.reversed().stream().filter(r -> r.bookId().equals(id)).toList());
                }
            }
            return out;
        }));
        server.createContext("/events", ex -> handle(ex, () -> {
            Map<String, String> q = query(ex);
            int after = Integer.parseInt(q.getOrDefault("after", "0"));
            long deadline = System.currentTimeMillis() + Long.parseLong(q.getOrDefault("waitMs", "0"));
            synchronized (LOCK) {
                while (REVIEWS.size() <= after && System.currentTimeMillis() < deadline)
                    LOCK.wait(Math.max(1, deadline - System.currentTimeMillis()));
                List<Review> out = List.copyOf(REVIEWS.subList(Math.min(after, REVIEWS.size()), REVIEWS.size()));
                EVENTS.addAndGet(out.size());
                return out;
            }
        }));
        server.createContext("/_fault", ex -> handle(ex, () -> {
            Map<String, String> q = query(ex);
            mode = q.getOrDefault("mode", "ok");
            slowMs = Long.parseLong(q.getOrDefault("ms", "2000"));
            return Map.of("mode", mode, "ms", slowMs);
        }));
        server.createContext("/_stats", ex -> handle(ex, () -> {
            if (ex.getRequestURI().getPath().endsWith("/reset")) { CALLS.set(0); EVENTS.set(0); }
            return Map.of("reviewCalls", CALLS.get(), "events", EVENTS.get(), "mode", mode);
        }));
        server.start();
        System.out.println("reviews service on :" + port);
    }

    interface Body { Object get() throws Exception; }

    static void handle(HttpExchange ex, Body body) throws IOException {
        try (ex) {
            Object out = body.get();
            if (out instanceof Integer status) { ex.sendResponseHeaders(status, -1); return; }
            byte[] bytes = JSON.writeValueAsBytes(out);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
        } catch (Exception e) {
            byte[] bytes = String.valueOf(e).getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(500, bytes.length);
            ex.getResponseBody().write(bytes);
        }
    }

    static Map<String, String> query(HttpExchange ex) {
        Map<String, String> q = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return q;
        for (String kv : raw.split("&")) {
            int i = kv.indexOf('=');
            if (i > 0) q.put(kv.substring(0, i), URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
        }
        return q;
    }
}
