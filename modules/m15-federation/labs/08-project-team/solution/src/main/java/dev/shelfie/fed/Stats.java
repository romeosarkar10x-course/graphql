package dev.shelfie.fed;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * What the router asked of this subgraph, for tests and experiments:
 *   GET  /_stats  {"subgraph", "requests", "entityCalls": {type: n}, "entityItems": {type: n}, "lastHeaders": {...}}
 *   POST /_reset  zero the counters and put the reviews back to the seed
 * A request is one HTTP POST to /graphql; an entity call is one invocation of an entity resolver, which may
 * resolve many representations (its items).
 */
@RestController
public class Stats extends OncePerRequestFilter {
    static final AtomicInteger REQUESTS = new AtomicInteger();
    static final Map<String, AtomicInteger> CALLS = new ConcurrentHashMap<>(), ITEMS = new ConcurrentHashMap<>();
    static volatile Map<String, String> lastHeaders = Map.of();
    static final List<String> WATCHED = List.of("authorization", "x-shelfie-user", "cookie", "x-request-id", "user-agent");

    private final String subgraph;
    private final long delayMs;

    Stats(@Value("${spring.profiles.active:none}") String subgraph, @Value("${shelfie.delay-ms:0}") long delayMs) {
        this.subgraph = subgraph;
        this.delayMs = delayMs;
    }

    /** Other state to put back on /_reset (the clubs' picks). */
    public static final List<Runnable> resetHooks = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Called by every entity resolver: one call, n representations. */
    public static void entity(String type, int n) {
        CALLS.computeIfAbsent(type, t -> new AtomicInteger()).incrementAndGet();
        ITEMS.computeIfAbsent(type, t -> new AtomicInteger()).addAndGet(n);
    }

    @GetMapping("/_stats")
    Map<String, Object> stats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("subgraph", subgraph);
        s.put("requests", REQUESTS.get());
        s.put("entityCalls", snapshot(CALLS));
        s.put("entityItems", snapshot(ITEMS));
        s.put("lastHeaders", lastHeaders);
        return s;
    }

    @PostMapping("/_reset")
    Map<String, Object> reset() {
        REQUESTS.set(0);
        CALLS.clear();
        ITEMS.clear();
        lastHeaders = Map.of();
        Data.resetReviews();
        resetHooks.forEach(Runnable::run);
        return stats();
    }

    private static Map<String, Integer> snapshot(Map<String, AtomicInteger> m) {
        Map<String, Integer> out = new TreeMap<>();
        m.forEach((k, v) -> out.put(k, v.get()));
        return out;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if ("/graphql".equals(req.getRequestURI()) && "POST".equals(req.getMethod())) {
            REQUESTS.incrementAndGet();
            Map<String, String> seen = new TreeMap<>();
            for (String h : WATCHED) if (req.getHeader(h) != null) seen.put(h, req.getHeader(h));
            lastHeaders = seen;
            if (delayMs > 0) {
                try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
        chain.doFilter(req, res);
    }
}
