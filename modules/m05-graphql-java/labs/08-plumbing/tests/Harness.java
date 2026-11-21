import dev.shelfie.plumbing.Plumbing;
import java.time.Duration;
import java.util.Map;

/** Lab 5.8 checks. Prints "pass <what>" / "fail <what> :: <detail>" lines for run.sh. */
public class Harness {
    static void check(boolean ok, String what, String detail) {
        System.out.println((ok ? "pass " : "fail ") + what + (ok ? "" : " :: " + detail));
    }

    public static void main(String[] args) {
        String q = "query ClubList { clubs { id name viewerIsMember } }";
        var cached = Plumbing.graphQL(true);
        int p0 = Plumbing.parseCount();
        for (int i = 0; i < 100; i++) cached.execute(Plumbing.input(q, "u1"));
        int parses = Plumbing.parseCount() - p0;
        check(parses == 1, "with the cache, 100 identical requests parse once", "parsed " + parses + " times");

        var plain = Plumbing.graphQL(false);
        p0 = Plumbing.parseCount();
        for (int i = 0; i < 100; i++) plain.execute(Plumbing.input(q, "u1"));
        parses = Plumbing.parseCount() - p0;
        check(parses == 100, "without the cache, every request parses", "parsed " + parses + " times");

        var fresh = Plumbing.graphQL(true);
        String vq = "query One($first: Int) { books { id } }";
        p0 = Plumbing.parseCount();
        for (int i = 0; i < 10; i++)
            fresh.execute(graphql.ExecutionInput.newExecutionInput(vq).variables(Map.of("first", i)).build());
        parses = Plumbing.parseCount() - p0;
        check(parses == 1, "different variables, same document: still one parse", "parsed " + parses + " times");

        var r = cached.execute(Plumbing.input("{ clubs { id viewerIsMember } }", "u5"));
        Object data = r.getData();
        String got = String.valueOf(data);
        check(got.equals("{clubs=[{id=c1, viewerIsMember=false}, {id=c2, viewerIsMember=true}, {id=c3, viewerIsMember=true}]}"),
            "viewerIsMember reads the viewer from GraphQLContext", got);
        r = cached.execute(Plumbing.input("{ clubs { id viewerIsMember } }", null));
        data = r.getData();
        got = String.valueOf(data);
        check(got.equals("{clubs=[{id=c1, viewerIsMember=null}, {id=c2, viewerIsMember=null}, {id=c3, viewerIsMember=null}]}"),
            "no viewer: viewerIsMember is null", got);

        int calls0 = Plumbing.slowTitleCalls();
        long t0 = System.nanoTime();
        r = Plumbing.executeWithin(cached, Plumbing.input("{ books { slowTitle } }", null), Duration.ofMillis(250));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        int started = Plumbing.slowTitleCalls() - calls0;
        boolean cancelled = Plumbing.errorMessages(r).stream().anyMatch(m -> m.toLowerCase().contains("cancel"));
        check(cancelled && ms < 700 && started < 10, "executeWithin cancels a 1 s query after 250 ms",
            ms + " ms, " + started + " of 10 slow fetchers started, errors " + Plumbing.errorMessages(r));
    }
}
