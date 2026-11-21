package dev.shelfie.plumbing;

import java.time.Duration;

/** The experiments: throughput with and without the document cache, then one cancelled slow query. */
public final class Main {
    public static void main(String[] args) {
        String q = "query ClubList { clubs { id name viewerIsMember } }";
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 20_000;
        for (boolean cache : new boolean[] {false, true}) {
            var graphQL = Plumbing.graphQL(cache);
            for (int i = 0; i < 2_000; i++) graphQL.execute(Plumbing.input(q, "u1"));   // warm-up
            int before = Plumbing.parseCount();
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) graphQL.execute(Plumbing.input(q, "u1"));
            double s = (System.nanoTime() - t0) / 1e9;
            System.out.printf("cache=%s %d requests %.0f req/s, %d parses%n", cache, n, n / s, Plumbing.parseCount() - before);
        }
        var graphQL = Plumbing.graphQL(true);
        long t0 = System.nanoTime();
        var r = Plumbing.executeWithin(graphQL, Plumbing.input("{ books { slowTitle } }", null), Duration.ofMillis(250));
        System.out.printf("cancelled after %d ms, %d of 10 slowTitle fetchers started, errors %s%n",
            (System.nanoTime() - t0) / 1_000_000, Plumbing.slowTitleCalls(), Plumbing.errorMessages(r));
    }
}
