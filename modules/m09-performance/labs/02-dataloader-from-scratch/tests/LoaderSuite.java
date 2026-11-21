import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/** The suite for lab 9.2. Prints "ok <name>" or "FAIL <name>: <why>" per case. Don't read it before you're done. */
public class LoaderSuite {
    static int failed = 0;

    /** A batch function that records every call and answers key -> "v" + key. */
    static class Recorder implements MiniLoader.Batch<Integer, String> {
        final List<List<Integer>> calls = Collections.synchronizedList(new ArrayList<>());
        Function<List<Integer>, CompletableFuture<List<MiniLoader.Result<String>>>> answer =
            keys -> CompletableFuture.completedFuture(keys.stream().map(k -> MiniLoader.Result.ok("v" + k)).toList());
        public CompletableFuture<List<MiniLoader.Result<String>>> load(List<Integer> keys) { calls.add(List.copyOf(keys)); return answer.apply(keys); }
    }

    interface Case { void run() throws Exception; }

    static void check(String name, Case c) {
        try { c.run(); System.out.println("ok   " + name); }
        catch (AssertionError | Exception e) { failed++; System.out.println("FAIL " + name + ": " + e.getMessage()); }
    }

    static void eq(Object want, Object got, String what) { if (!want.equals(got)) throw new AssertionError(what + ": want " + want + ", got " + got); }

    static Throwable cause(CompletableFuture<?> f) throws Exception {
        try { f.get(2, TimeUnit.SECONDS); } catch (ExecutionException e) { return e.getCause(); }
        throw new AssertionError("expected the future to fail, it completed");
    }

    public static void main(String[] args) {
        check("nothing is sent before dispatch()", () -> {
            var r = new Recorder(); var l = new MiniLoader<>(r);
            var f = l.load(1); l.load(2);
            eq(0, r.calls.size(), "batch calls"); eq(false, f.isDone(), "future done");
        });
        check("dispatch() sends one batch with every key, in first-request order", () -> {
            var r = new Recorder(); var l = new MiniLoader<>(r);
            var a = l.load(3); var b = l.load(1); var c = l.load(2);
            eq(3, l.dispatch(), "keys sent"); eq(List.of(List.of(3, 1, 2)), r.calls, "batch calls");
            eq("v3", a.get(2, TimeUnit.SECONDS), "a"); eq("v1", b.get(2, TimeUnit.SECONDS), "b"); eq("v2", c.get(2, TimeUnit.SECONDS), "c");
        });
        check("a key asked for twice is sent once, and both callers get it", () -> {
            var r = new Recorder(); var l = new MiniLoader<>(r);
            var a = l.load(7); var b = l.load(7); l.load(8);
            l.dispatch(); eq(List.of(List.of(7, 8)), r.calls, "batch calls");
            eq("v7", a.get(2, TimeUnit.SECONDS), "first"); eq("v7", b.get(2, TimeUnit.SECONDS), "second");
        });
        check("a loaded key is remembered: no second batch", () -> {
            var r = new Recorder(); var l = new MiniLoader<>(r);
            l.load(5); l.dispatch();
            var again = l.load(5);
            eq(0, l.dispatch(), "keys sent the second time"); eq(1, r.calls.size(), "batch calls"); eq("v5", again.get(2, TimeUnit.SECONDS), "value");
        });
        check("dispatch() with nothing queued doesn't call the batch function", () -> {
            var r = new Recorder(); var l = new MiniLoader<>(r);
            eq(0, l.dispatch(), "keys sent"); eq(0, r.calls.size(), "batch calls");
        });
        check("an error for one key fails only that key", () -> {
            var r = new Recorder();
            r.answer = keys -> CompletableFuture.completedFuture(keys.stream().map(k -> k == 2
                ? MiniLoader.Result.<String>fail(new IllegalArgumentException("no book 2")) : MiniLoader.Result.ok("v" + k)).toList());
            var l = new MiniLoader<>(r);
            var a = l.load(1); var b = l.load(2); var c = l.load(3); l.dispatch();
            eq("v1", a.get(2, TimeUnit.SECONDS), "key 1"); eq("no book 2", cause(b).getMessage(), "key 2's error"); eq("v3", c.get(2, TimeUnit.SECONDS), "key 3");
        });
        check("a failed batch fails every key in it, with its cause", () -> {
            var r = new Recorder(); r.answer = keys -> CompletableFuture.failedFuture(new IllegalStateException("database down"));
            var l = new MiniLoader<>(r);
            var a = l.load(1); var b = l.load(2); l.dispatch();
            eq("database down", cause(a).getMessage(), "key 1"); eq("database down", cause(b).getMessage(), "key 2");
        });
        check("a batch function that throws fails the keys, not dispatch()", () -> {
            var r = new Recorder(); r.answer = keys -> { throw new IllegalStateException("bug in the batch function"); };
            var l = new MiniLoader<>(r);
            var a = l.load(1); l.dispatch();
            eq("bug in the batch function", cause(a).getMessage(), "key 1");
        });
        check("a batch that answers the wrong number of keys fails them all", () -> {
            var r = new Recorder(); r.answer = keys -> CompletableFuture.completedFuture(List.of(MiniLoader.Result.ok("only one")));
            var l = new MiniLoader<>(r);
            var a = l.load(1); var b = l.load(2); l.dispatch();
            cause(a); cause(b);
        });
        check("maxBatchSize splits a dispatch into several batches", () -> {
            var r = new Recorder(); var l = new MiniLoader<>(r, 2);
            for (int k = 1; k <= 5; k++) l.load(k);
            eq(5, l.dispatch(), "keys sent"); eq(List.of(List.of(1, 2), List.of(3, 4), List.of(5)), r.calls, "batch calls");
        });
        check("a primed key is never sent", () -> {
            var r = new Recorder(); var l = new MiniLoader<>(r);
            l.prime(4, "primed");
            var f = l.load(4); l.load(6); l.dispatch();
            eq(List.of(List.of(6)), r.calls, "batch calls"); eq("primed", f.get(2, TimeUnit.SECONDS), "value");
        });
        check("clear(key) makes the next load fetch it again", () -> {
            var r = new Recorder(); var l = new MiniLoader<>(r);
            l.load(9); l.dispatch(); l.clear(9); l.load(9); l.dispatch();
            eq(List.of(List.of(9), List.of(9)), r.calls, "batch calls");
        });
        check("a failure is not remembered: the next load tries again", () -> {
            var r = new Recorder(); r.answer = keys -> CompletableFuture.failedFuture(new IllegalStateException("down"));
            var l = new MiniLoader<>(r);
            var first = l.load(1); l.dispatch(); cause(first);
            r.answer = keys -> CompletableFuture.completedFuture(keys.stream().map(k -> MiniLoader.Result.ok("v" + k)).toList());
            var retry = l.load(1); eq(1, l.dispatch(), "keys sent on the retry");
            eq("v1", retry.get(2, TimeUnit.SECONDS), "after the retry");
        });
        check("a load made while a batch completes waits for the next dispatch()", () -> {
            var r = new Recorder(); var l = new MiniLoader<>(r);
            List<CompletableFuture<String>> chained = new ArrayList<>();
            l.load(1).thenAccept(v -> chained.add(l.load(2)));
            l.dispatch();
            eq(List.of(List.of(1)), r.calls, "batch calls after the first dispatch");
            eq(1, chained.size(), "the chained load happened"); eq(false, chained.getFirst().isDone(), "chained future done");
            eq(1, l.dispatch(), "keys sent by the second dispatch"); eq("v2", chained.getFirst().get(2, TimeUnit.SECONDS), "chained value");
        });
        check("loadMany answers in the order asked", () -> {
            var r = new Recorder(); var l = new MiniLoader<>(r);
            var all = l.loadMany(List.of(3, 1, 3, 2)); l.dispatch();
            eq(List.of("v3", "v1", "v3", "v2"), all.get(2, TimeUnit.SECONDS), "values"); eq(List.of(List.of(3, 1, 2)), r.calls, "batch calls");
        });
        check("an asynchronous batch completes the futures later", () -> {
            var r = new Recorder();
            r.answer = keys -> CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(50); } catch (InterruptedException e) { throw new RuntimeException(e); }
                return keys.stream().map(k -> MiniLoader.Result.ok("late" + k)).toList();
            });
            var l = new MiniLoader<>(r);
            var f = l.load(1); l.dispatch();
            eq(false, f.isDone(), "done right after dispatch()"); eq("late1", f.get(2, TimeUnit.SECONDS), "value");
        });
        System.out.println(failed == 0 ? "all 16 cases pass" : failed + " of 16 cases fail");
        System.exit(failed == 0 ? 0 : 1);
    }
}
