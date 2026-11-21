import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Lab 8.8 harness: the three modes on Page.QUERY, and what an error-aware cache keeps from each. */
public class OnErrorCheck {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static int fails = 0;
    static void check(boolean ok, String what) { System.out.println((ok ? "ok    " : "FAIL  ") + what); if (!ok) fails++; }

    static JsonNode run(OnError.Mode mode, String query) {
        Page.FETCHES.set(0);
        return JSON.valueToTree(OnError.execute(Page.graphQL(), query, mode).toSpecification());
    }
    static JsonNode path(Object... segments) { return JSON.valueToTree(List.of(segments)); }

    public static void main(String[] args) {
        JsonNode p = run(OnError.Mode.PROPAGATE, Page.QUERY);
        check(p.path("data").isNull() && p.path("errors").size() == 2, "PROPAGATE: data null, both errors (the default, unchanged)");

        JsonNode n;
        try { n = run(OnError.Mode.NULL, Page.QUERY); } catch (UnsupportedOperationException e) { check(false, "NULL is implemented"); n = JSON.createObjectNode(); }
        check(n.at("/data/club/name").asString("").equals("The Midnight Readers"), "NULL: the club survives");
        check(n.at("/data/club/currentBook").isObject() && n.at("/data/club/currentBook/rating").isNull()
            && n.at("/data/club/currentBook/title").asString("").equals("Piranesi"), "NULL: rating is null in place, title kept");
        check(n.at("/data/club/members").size() == 3 && n.at("/data/club/members/2/name").isNull()
            && n.at("/data/club/members/1/name").asString("").equals("Ben"), "NULL: members[2].name is null in place, the list survives");
        check(n.path("errors").size() == 2 && n.at("/errors/0/path").equals(path("club", "currentBook", "rating"))
            && n.at("/errors/1/path").equals(path("club", "members", 2, "name")), "NULL: the same two errors, same paths");
        check(n.at("/errors/0/locations/0/line").asInt() == 1 && n.at("/errors/0/locations/0/column").asInt() == 51,
            "NULL: error locations still point into the client's query (line 1, column 51)");
        JsonNode syntax;
        try { syntax = run(OnError.Mode.NULL, "{ club(id: \"c1\") { name "); } catch (RuntimeException e) { syntax = JSON.createObjectNode(); }
        check(!syntax.has("data") && syntax.at("/errors/0/extensions/classification").asString("").equals("InvalidSyntax"),
            "NULL: a syntax error is still a request error");

        JsonNode h;
        try { h = run(OnError.Mode.HALT, Page.QUERY); } catch (UnsupportedOperationException e) { check(false, "HALT is implemented"); h = JSON.createObjectNode(); }
        int fetches = Page.FETCHES.get();
        check(h.has("data") && h.path("data").isNull() && h.path("errors").size() == 1
            && h.at("/errors/0/path").equals(path("club", "currentBook", "rating")), "HALT: data null and only the first error");
        check(fetches <= 3, "HALT: execution stops at the first error (" + fetches + " fetchers ran, at most 3)");

        var counts = new java.util.LinkedHashMap<String, Integer>();
        for (var e : Map.of("PROPAGATE", p, "NULL", n, "HALT", h).entrySet()) {
            var cache = new NormalizedCache();
            counts.put(e.getKey(), cache.write(e.getValue()));
            if (e.getKey().equals("NULL")) {
                check(cache.records().size() == 6, "cache after NULL: 6 records (c1, b1, u11, u1, u2, u3), got " + cache.records().size());
                check(cache.records().containsKey("b1") && !cache.records().get("b1").containsKey("rating")
                    && cache.records().containsKey("u3") && !cache.records().get("u3").containsKey("name"),
                    "cache after NULL: the errored fields are not written (no b1.rating, no u3.name)");
            }
        }
        check(counts.get("PROPAGATE") == 0 && counts.get("HALT") == 0 && counts.get("NULL") == 14,
            "fields cached: PROPAGATE " + counts.get("PROPAGATE") + ", NULL " + counts.get("NULL") + ", HALT " + counts.get("HALT") + " (expected 0, 14, 0)");
        System.exit(fails == 0 ? 0 : 1);
    }
}
