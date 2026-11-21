import java.util.*;
import java.util.concurrent.*;
import org.dataloader.*;

/** java-dataloader 6.0.0: is a failed key remembered? Does a per-key Try fail only that key? */
public class FailCache {
    public static void main(String[] args) throws Exception {
        List<List<Integer>> calls = new ArrayList<>();
        DataLoader<Integer, String> l = DataLoaderFactory.newDataLoader((List<Integer> keys) -> {
            calls.add(List.copyOf(keys));
            return CompletableFuture.failedFuture(new IllegalStateException("down"));
        });
        var a = l.load(1); l.dispatch();
        try { a.get(); } catch (ExecutionException e) { System.out.println("first: failed " + e.getCause().getMessage()); }
        var b = l.load(1); l.dispatch();
        System.out.println("second load done=" + b.isDone() + " failed=" + b.isCompletedExceptionally() + " batch calls=" + calls);
        l.clear(1); l.load(1); l.dispatch();
        System.out.println("after clear: batch calls=" + calls);
        DataLoader<Integer, String> t = DataLoaderFactory.newDataLoaderWithTry((List<Integer> keys) -> CompletableFuture.completedFuture(
            keys.stream().map(k -> k == 2 ? org.dataloader.Try.<String>failed(new IllegalArgumentException("no 2")) : org.dataloader.Try.succeeded("v" + k)).toList()));
        var x = t.load(1); var y = t.load(2); t.dispatch();
        System.out.println("try: key1=" + x.get() + " key2 failed=" + y.isCompletedExceptionally());
    }
}
