import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A DataLoader. Today it is the N+1 loader: every load() is its own batch of one, sent at once.
 * Make it queue keys, send them in one batch on dispatch(), and remember every answer (README).
 */
public final class MiniLoader<K, V> {
    /** One key's outcome: a value, or an error for that key alone. */
    public record Result<V>(V value, Throwable error) {
        public static <V> Result<V> ok(V value) { return new Result<>(value, null); }
        public static <V> Result<V> fail(Throwable error) { return new Result<>(null, error); }
    }

    /** Loads many keys in one call. The list answers the keys in the order they were given. */
    public interface Batch<K, V> { CompletableFuture<List<Result<V>>> load(List<K> keys); }

    private final Batch<K, V> batch;

    public MiniLoader(Batch<K, V> batch) { this(batch, Integer.MAX_VALUE); }

    public MiniLoader(Batch<K, V> batch, int maxBatchSize) { this.batch = batch; }

    public CompletableFuture<V> load(K key) {
        return batch.load(List.of(key)).thenApply(results -> results.getFirst().value());
    }

    public CompletableFuture<List<V>> loadMany(List<K> keys) {
        List<CompletableFuture<V>> all = keys.stream().map(this::load).toList();
        return CompletableFuture.allOf(all.toArray(CompletableFuture[]::new)).thenApply(x -> all.stream().map(CompletableFuture::join).toList());
    }

    /** Sends everything queued. Returns the number of keys sent. */
    public int dispatch() { return 0; }

    public MiniLoader<K, V> clear(K key) { return this; }

    public MiniLoader<K, V> clearAll() { return this; }

    public MiniLoader<K, V> prime(K key, V value) { return this; }
}
