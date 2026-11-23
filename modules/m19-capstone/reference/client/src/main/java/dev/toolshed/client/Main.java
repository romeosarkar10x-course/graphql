package dev.toolshed.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.client.FieldAccessException;

/**
 * The Toolshed command line (the course's Java client). TOOLSHED_URL (default http://localhost:4000/graphql)
 * and TOOLSHED_TOKEN (a bearer token: bin/course token ada --aud toolshed-api).
 *
 *   tools [--first N] [--powered|--hand] [--category ID] [--after CURSOR]
 *   loans                    the signed-in member's loans
 *   borrow <toolId> [--days N]
 *   return <loanId>
 *   ui [--port 4005]         serve ui/index.html and pass /graphql to the router
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: toolshed tools|loans|borrow|return|ui [options] (see Main.java)");
            System.exit(2);
        }
        String url = System.getenv().getOrDefault("TOOLSHED_URL", "http://localhost:4000/graphql");
        Toolshed shed = new Toolshed(url, System.getenv("TOOLSHED_TOKEN"));
        Map<String, String> opts = options(args);
        try {
            switch (args[0]) {
                case "tools" -> {
                    Map<String, Object> filter = new HashMap<>();
                    if (opts.containsKey("powered")) filter.put("powered", true);
                    if (opts.containsKey("hand")) filter.put("powered", false);
                    if (opts.containsKey("category")) filter.put("categoryId", opts.get("category"));
                    var page = shed.tools(Integer.parseInt(opts.getOrDefault("first", "10")), opts.get("after"), filter.isEmpty() ? null : filter);
                    System.out.printf("%d tools%n", page.totalCount());
                    for (var e : page.edges()) {
                        var t = e.node();
                        System.out.printf("  %-4s %-24s %-14s %-18s %s%n", t.id(), t.name(), t.specs().summary(), t.branch().name(),
                                t.available() == null ? "ask at the desk" : t.available() ? "on the shelf" : "out");
                    }
                    if (page.pageInfo().hasNextPage()) System.out.println("  more: --after " + page.pageInfo().endCursor());
                }
                case "loans" -> {
                    var me = shed.me();
                    if (me == null) { System.out.println("signed out: set TOOLSHED_TOKEN"); return; }
                    System.out.println(me.displayName() + " <" + me.email() + ">");
                    for (var e : me.loans().edges()) {
                        var l = e.node();
                        System.out.printf("  %-5s %-8s %-24s due %s%n", l.id(), l.status(), l.tool().name(), l.dueAt().toLocalDate());
                    }
                }
                case "borrow" -> {
                    var r = shed.borrow(args[1], Integer.parseInt(opts.getOrDefault("days", "7")));
                    switch (r.__typename()) {
                        case "Borrowed" -> System.out.println("borrowed: loan " + r.loan().get("id") + ", due " + r.loan().get("dueAt"));
                        default -> { System.out.println(r.__typename() + ": " + r.message()); System.exit(3); }
                    }
                }
                case "return" -> System.out.println(shed.giveBack(args[1]));
                case "ui" -> Ui.serve(Integer.parseInt(opts.getOrDefault("port", "4005")), url);
                default -> { System.err.println("unknown command " + args[0]); System.exit(2); }
            }
        } catch (FieldAccessException e) {
            System.err.println("error: " + e.getResponse().getErrors().stream().map(err -> err.getMessage()).toList());
            System.exit(1);
        }
    }

    static Map<String, String> options(String[] args) {
        Map<String, String> o = new HashMap<>();
        List<String> flags = List.of("powered", "hand");
        for (int i = 1; i < args.length; i++) {
            if (!args[i].startsWith("--")) continue;
            String k = args[i].substring(2);
            o.put(k, flags.contains(k) || i + 1 >= args.length ? "" : args[++i]);
        }
        return o;
    }
}
