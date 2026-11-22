package dev.shelfie.gateway;

import dev.shelfie.gateway.OpenLibrary.Edition;
import dev.shelfie.gateway.OpenLibrary.Search;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Answers from Open Library, kept for ttl. "No such ISBN" is an answer too and is kept the same way, or every page
 * that mentions a missing ISBN would call again. Older answers, up to stale-for, stand in while Open Library fails.
 */
@Component
public class Cache {
    /** What a key resolves to: an edition (or none), whether it is stale, or why there is nothing. */
    public record Lookup(Edition edition, String error) {}

    record Entry<T>(T value, Instant at) {}

    private final OpenLibrary openLibrary;
    private final Duration ttl, staleFor;
    private final Map<String, Entry<Optional<Edition>>> editions = new ConcurrentHashMap<>();
    private final Map<String, Entry<Search>> searches = new ConcurrentHashMap<>();

    Cache(OpenLibrary openLibrary, @Value("${gateway.ttl}") Duration ttl, @Value("${gateway.stale-for}") Duration staleFor) {
        this.openLibrary = openLibrary;
        this.ttl = ttl;
        this.staleFor = staleFor;
    }

    boolean fresh(Entry<?> e) { return e != null && e.at().plus(ttl).isAfter(Instant.now()); }
    boolean usable(Entry<?> e) { return e != null && e.at().plus(staleFor).isAfter(Instant.now()); }

    /** Called by the DataLoader with every ISBN one request needs: at most one Open Library call. */
    public Map<String, Lookup> editions(Set<String> isbns) {
        Map<String, Lookup> out = new HashMap<>();
        Set<String> missing = new LinkedHashSet<>();
        for (String isbn : isbns) {
            var e = editions.get(isbn);
            if (fresh(e)) out.put(isbn, new Lookup(e.value().orElse(null), null));
            else missing.add(isbn);
        }
        if (missing.isEmpty()) return out;
        try {
            Map<String, Edition> found = openLibrary.editions(missing);
            Instant now = Instant.now();
            for (String isbn : missing) {
                Optional<Edition> value = Optional.ofNullable(found.get(isbn));
                editions.put(isbn, new Entry<>(value, now));
                out.put(isbn, new Lookup(value.orElse(null), null));
            }
        } catch (OpenLibrary.Unavailable failed) {
            for (String isbn : missing) {
                var e = editions.get(isbn);
                out.put(isbn, usable(e) ? new Lookup(e.value().map(Edition::asStale).orElse(null), null) : new Lookup(null, failed.getMessage()));
            }
        }
        return out;
    }

    public Search search(String text, int first) {
        String key = text.trim().toLowerCase() + "|" + first;
        var e = searches.get(key);
        if (fresh(e)) return e.value();
        try {
            Search s = openLibrary.search(text.trim(), first);
            searches.put(key, new Entry<>(s, Instant.now()));
            return s;
        } catch (OpenLibrary.Unavailable failed) {
            if (usable(e)) return e.value();
            throw failed;
        }
    }
}
