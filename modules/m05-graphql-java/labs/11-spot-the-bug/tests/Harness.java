import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Game 5b checks, one per bug. Prints "pass B<n> ..." / "fail B<n> ... :: detail". Run next to Bugs.java. */
public class Harness {
    static GraphQL g;

    static void check(String id, String what, Callable<String> test) {
        String problem;
        try { problem = test.call(); } catch (Throwable t) { problem = t.toString(); }
        System.out.println((problem == null ? "pass " : "fail ") + id + " " + what + (problem == null ? "" : " :: " + problem));
    }

    static ExecutionResult run(String q, String viewer) throws Exception {
        return g.executeAsync(Bugs.input(q, viewer)).get(5, TimeUnit.SECONDS);
    }

    static String expect(ExecutionResult r, String want) {
        if (!r.getErrors().isEmpty()) return "errors: " + r.getErrors().get(0).getMessage();
        Object data = r.getData();
        return String.valueOf(data).equals(want) ? null : "got " + data;
    }

    public static void main(String[] args) throws Exception {
        g = Bugs.graphQL();
        check("B1", "slow calls stay off the common pool", () -> {
            Bugs.RATING_THREADS.clear();
            run("{ books { averageRating } }", null);
            var bad = Bugs.RATING_THREADS.stream().filter(n -> n.contains("ForkJoinPool.commonPool")).toList();
            return bad.isEmpty() ? null : "ratingsService ran on " + bad.get(0);
        });
        check("B2", "label is right under 32 concurrent requests", () -> {
            String want = "{clubs=[{label=The Midnight Readers (8)}, {label=Slow Burn Mysteries (4)}, {label=Booker Shortlist Survivors (5)}]}";
            try (var pool = Executors.newFixedThreadPool(32)) {
                var futures = new ArrayList<java.util.concurrent.Future<String>>();
                for (int i = 0; i < 2000; i++) futures.add(pool.submit(() -> expect(run("{ clubs { label } }", null), want)));
                for (var f : futures) { String p = f.get(); if (p != null) return p; }
            }
            return null;
        });
        check("B3", "Review.book works with the real source type", () ->
            expect(run("{ books(first: 1) { reviews { book { id } } } }", null),
                "{books=[{reviews=[{book={id=b1}}, {book={id=b1}}, {book={id=b1}}, {book={id=b1}}, {book={id=b1}}]}]}"));
        check("B4", "membersByName does not reorder the store", () -> {
            run("{ club(id: \"c1\") { membersByName { id } } }", null);
            return expect(run("{ club(id: \"c1\") { members { id } } }", null),
                "{club={members=[{id=u11}, {id=u1}, {id=u2}, {id=u3}, {id=u4}, {id=u6}, {id=u8}, {id=u10}]}}");
        });
        check("B5", "books(first:) survives null and large values", () -> {
            for (String q : List.of("{ books(first: null) { id } }", "{ books(first: 50) { id } }")) {
                var r = run(q, null);
                if (!r.getErrors().isEmpty()) return q + " -> " + r.getErrors().get(0).getMessage();
                Object data = r.getData();
                if (String.valueOf(data).split("id=").length - 1 != 10) return q + " -> " + data;
            }
            return null;
        });
        check("B6", "owner resolves, and the club is not nulled", () ->
            expect(run("{ club(id: \"c1\") { name owner { name } } }", null), "{club={name=The Midnight Readers, owner={name=Kemi Adeyemi}}}"));
        check("B7", "viewerIsMember sees the viewer on any thread", () ->
            expect(run("{ clubs { id viewerIsMember } }", "u5"),
                "{clubs=[{id=c1, viewerIsMember=false}, {id=c2, viewerIsMember=true}, {id=c3, viewerIsMember=true}]}"));
        check("B8", "currentBook for every club finishes (no pool starvation)", () ->
            expect(run("{ clubs { currentBook { title } } }", null),
                "{clubs=[{currentBook={title=The Left Hand of Darkness}}, {currentBook={title=The Name of the Rose}}, {currentBook={title=The Seven Moons of Maali Almeida}}]}"));
        System.exit(0);   // a starved pool would keep the JVM alive
    }
}
