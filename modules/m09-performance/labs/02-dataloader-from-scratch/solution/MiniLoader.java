import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** A DataLoader in 60 lines: queue keys, send them in one batch on dispatch(), remember every answer. */
public final class MiniLoader<K, V> {
    /** One key's outcome: a value, or an error for that key alone. */
    public record Result<V>(V value, Throwable error) {
        public static <V> Result<V> ok(V value) { return new Result<>(value, null); }
        public static <V> Result<V> fail(Throwable error) { return new Result<>(null, error); }
    }

    /** Loads many keys in one call. The list answers the keys in the order they were given. */
    public interface Batch<K, V> { CompletableFuture<List<Result<V>>> load(List<K> keys); }

    private final Batch<K, V> batch;
    private final int maxBatchSize;
    private final Map<K, CompletableFuture<V>> memo = new HashMap<>();             // every key ever loaded
    private final Map<K, CompletableFuture<V>> queue = new LinkedHashMap<>();      // keys not sent yet, in order

    public MiniLoader(Batch<K, V> batch) { this(batch, Integer.MAX_VALUE); }

    public MiniLoader(Batch<K, V> batch, int maxBatchSize) {
        if (maxBatchSize < 1) throw new IllegalArgumentException("maxBatchSize must be at least 1");
        this.batch = batch;
        this.maxBatchSize = maxBatchSize;
    }

    public synchronized CompletableFuture<V> load(K key) {
        return memo.computeIfAbsent(key, k -> { var f = new CompletableFuture<V>(); queue.put(k, f); return f; });
    }

    public CompletableFuture<List<V>> loadMany(List<K> keys) {
        List<CompletableFuture<V>> all = keys.stream().map(this::load).toList();
        return CompletableFuture.allOf(all.toArray(CompletableFuture[]::new)).thenApply(x -> all.stream().map(CompletableFuture::join).toList());
    }

    /** Sends everything queued, in batches of at most maxBatchSize. Returns the number of keys sent. */
    public int dispatch() {
        List<Map.Entry<K, CompletableFuture<V>>> pending;
        synchronized (this) { pending = new ArrayList<>(queue.entrySet()); queue.clear(); }
        for (int i = 0; i < pending.size(); i += maxBatchSize)
            send(pending.subList(i, Math.min(i + maxBatchSize, pending.size())));
        return pending.size();
    }

    private void send(List<Map.Entry<K, CompletableFuture<V>>> part) {
        List<K> keys = part.stream().map(Map.Entry::getKey).toList();
        CompletableFuture<List<Result<V>>> call;
        try { call = batch.load(keys); } catch (Throwable t) { call = CompletableFuture.failedFuture(t); }
        call.whenComplete((results, error) -> {
            Throwable all = error != null ? error : results == null || results.size() != keys.size()
                ? new IllegalStateException("batch answered " + (results == null ? 0 : results.size()) + " of " + keys.size() + " keys") : null;
            for (int i = 0; i < part.size(); i++) {
                var f = part.get(i).getValue();
                Throwable e = all != null ? all : results.get(i).error();
                if (e == null) { f.complete(results.get(i).value()); continue; }
                synchronized (this) { memo.remove(part.get(i).getKey(), f); }   // failures aren't remembered: the next load retries
                f.completeExceptionally(e);
            }
        });
    }

    /** Forget a key, after a write. */
    public synchronized MiniLoader<K, V> clear(K key) { memo.remove(key); queue.remove(key); return this; }

    public synchronized MiniLoader<K, V> clearAll() { memo.clear(); queue.clear(); return this; }

    /** Seed a value you already have; an existing entry wins. */
    public synchronized MiniLoader<K, V> prime(K key, V value) { memo.putIfAbsent(key, CompletableFuture.completedFuture(value)); return this; }
}
