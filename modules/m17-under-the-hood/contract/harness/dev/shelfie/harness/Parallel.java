package dev.shelfie.harness;

import dev.shelfie.mini.MiniGraphQL;
import dev.shelfie.mini.Schema;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Lab 17.5's parallel check: four slow root fields and a list of three items with one slow field each, every slow
 * resolver sleeping 200 ms. In parallel (fields and list items) the query takes about 200 ms; one after another,
 * 1,400 ms. Prints {"ms": ..., "virtual": n, "slow": 7, "result": ...}: n is how many of the 7 slow resolvers ran on
 * a virtual thread.
 */
public final class Parallel {
    public static void main(String[] a) throws Exception {
        String sdl = Files.readString(Path.of(a[0]));
        Map<String, Object> slow = Map.of("$sleep", 200, "value", "x");
        Map<String, Object> child = Map.of("name", slow);
        Map<String, Object> data = Map.of("ok", slow, "children", List.of(child, child, child));
        FixtureResolver r = new FixtureResolver();
        Schema schema = new Schema(sdl);
        String q = "{ a: ok b: ok c: ok d: ok children { name } }";
        MiniGraphQL.execute(schema, q, null, null, new FixtureResolver(), data);   // warm up
        long t0 = System.nanoTime();
        Map<String, Object> result = MiniGraphQL.execute(schema, q, null, null, r, data);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        boolean right = result.toString().equals("{data={a=x, b=x, c=x, d=x, children=[{name=x}, {name=x}, {name=x}]}}");
        System.out.println("{\"ms\": " + ms + ", \"virtual\": " + r.virtual.get() + ", \"slow\": 7, \"result\": " + (right ? "\"right\"" : "\"" + result + "\"") + "}");
    }
}
