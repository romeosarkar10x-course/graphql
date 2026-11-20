package dev.shelfie.ref;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Course plumbing, not part of Shelfie: counts requests to /api and /graphql so a lab grader can
 * check that a client made the number of HTTP calls it claims (Module 1.2 and 1.3).
 *
 *   GET  /_course/stats   -> {"api":N,"graphql":M}
 *   POST /_course/reset   -> zeroes both counters
 *   POST /_course/reset-data -> puts the Store back to its seed (Module 4 writes)
 */
@Component
public class CourseStatsFilter extends OncePerRequestFilter {

    private final AtomicLong api = new AtomicLong();
    private final dev.shelfie.ref.data.Store store;

    public CourseStatsFilter(dev.shelfie.ref.data.Store store) {
        this.store = store;
    }

    private final AtomicLong graphql = new AtomicLong();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        if (path.equals("/_course/stats")) {
            res.setContentType("application/json");
            res.getWriter().write("{\"api\":" + api.get() + ",\"graphql\":" + graphql.get() + "}");
            return;
        }
        if (path.equals("/_course/reset") && req.getMethod().equals("POST")) {
            api.set(0);
            graphql.set(0);
            res.setStatus(204);
            return;
        }
        if (path.equals("/_course/reset-data") && req.getMethod().equals("POST")) {
            store.reset();
            res.setStatus(204);
            return;
        }
        if (path.startsWith("/api/")) api.incrementAndGet();
        else if (path.equals("/graphql")) graphql.incrementAndGet();
        chain.doFilter(req, res);
    }
}
