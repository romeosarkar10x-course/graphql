package dev.shelfie.subgraphs;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Provided: GET /_stats {"queries": n, "byMethod": {...}}, POST /_reset. The grader reads it; do not change it. */
@RestController
class StatsController {
    @GetMapping("/_stats")
    synchronized Map<String, Object> stats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("queries", Store.QUERIES.get());
        Map<String, Integer> by = new LinkedHashMap<>();
        Store.BY_METHOD.forEach((k, v) -> by.put(k, v.get()));
        s.put("byMethod", by);
        return s;
    }

    @PostMapping("/_reset")
    synchronized Map<String, Object> reset() {
        Store.QUERIES.set(0);
        Store.BY_METHOD.clear();
        return stats();
    }
}
