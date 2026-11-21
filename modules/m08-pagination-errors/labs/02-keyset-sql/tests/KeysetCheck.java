import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Lab 8.2 harness: Keyset.java against 1,000,000 books in m08_02.book, ordered by (published, id). */
public class KeysetCheck {
    static int fails = 0;
    static void check(boolean ok, String what) { System.out.println((ok ? "ok    " : "FAIL  ") + what); if (!ok) fails++; }

    record Row(int published, long id) {}

    public static void main(String[] args) throws Exception {
        String url = System.getenv().getOrDefault("PGURL", "jdbc:postgresql://postgres:5432/shelfie") + "?currentSchema=m08_02";
        try (Connection c = DriverManager.getConnection(url, "shelfie", "shelfie")) {
            cursors();
            paging(c);
            plans(c);
            timing(c);
        }
        System.exit(fails == 0 ? 0 : 1);
    }

    static Row rowAt(Connection c, int offset) throws SQLException {
        try (var s = c.createStatement(); var rs = s.executeQuery("select published, id from book order by published, id offset " + offset + " limit 1")) {
            rs.next();
            return new Row(rs.getInt(1), rs.getLong(2));
        }
    }

    static List<Long> expected(Connection c, int offset, int limit) throws SQLException {
        var out = new ArrayList<Long>();
        try (var s = c.createStatement(); var rs = s.executeQuery("select id from book order by published, id offset " + offset + " limit " + limit)) {
            while (rs.next()) out.add(rs.getLong(1));
        }
        return out;
    }

    static String cursorAt(Connection c, int offset) throws SQLException {
        var r = rowAt(c, offset);
        return Keyset.encode(r.published(), r.id());
    }

    static void cursors() {
        String cur = Keyset.encode(1999, 424242);
        var back = Keyset.decode(cur);
        check(back.published() == 1999 && back.id() == 424242, "decode(encode(1999, 424242)) gives the position back");
        check(!cur.contains("1999") && !cur.contains("424242"), "cursors are opaque: the sort key is not readable in " + cur);
        for (String bad : new String[]{"hello", "1999:424242", "", "djE6MTk5OQ"}) {
            boolean threw;
            try { Keyset.decode(bad); threw = false; } catch (IllegalArgumentException e) { threw = true; } catch (RuntimeException e) { threw = false; }
            check(threw, "decode(\"" + bad + "\") throws IllegalArgumentException");
        }
    }

    static void paging(Connection c) throws SQLException {
        var p = Keyset.forward(c, 20, null);
        check(p.ids().equals(expected(c, 0, 20)), "forward(20, null) is the first 20 rows");
        check(p.hasNextPage() && !p.hasPreviousPage(), "first page: hasNextPage true, hasPreviousPage false");

        var walked = new ArrayList<Long>();
        String after = cursorAt(c, 499_999);
        for (int i = 0; i < 3; i++) {
            var page = Keyset.forward(c, 50, after);
            walked.addAll(page.ids());
            after = page.endCursor();
            if (i == 0) check(page.hasPreviousPage(), "a page after a cursor: hasPreviousPage true");
        }
        check(walked.equals(expected(c, 500_000, 150)), "three pages of 50 from offset 500,000 match OFFSET's rows, no gap, no duplicate");

        var end = Keyset.forward(c, 10, cursorAt(c, 999_989));
        check(end.ids().equals(expected(c, 999_990, 10)) && !end.hasNextPage(), "the last 10 rows: hasNextPage false");
        var past = Keyset.forward(c, 10, cursorAt(c, 999_999));
        check(past.ids().isEmpty() && !past.hasNextPage() && past.endCursor() == null, "after the last row: no ids, no cursors");

        Keyset.Page b;
        try { b = Keyset.backward(c, 25, cursorAt(c, 500_000)); }
        catch (UnsupportedOperationException e) { check(false, "backward(...) is implemented"); return; }
        check(b.ids().equals(expected(c, 499_975, 25)), "backward(25, before row 500,000) is rows 499,975..499,999 in ascending order");
        check(b.hasNextPage() && b.hasPreviousPage(), "a page before a cursor in the middle: hasNextPage and hasPreviousPage true");
        var again = Keyset.backward(c, 25, b.startCursor());
        check(again.ids().equals(expected(c, 499_950, 25)), "backward again from startCursor: the 25 rows before that");
        var start = Keyset.backward(c, 25, cursorAt(c, 10));
        check(start.ids().equals(expected(c, 0, 10)) && !start.hasPreviousPage(), "backward near the start: 10 rows, hasPreviousPage false");
        var tail = Keyset.backward(c, 5, null);
        check(tail.ids().equals(expected(c, 999_995, 5)), "backward(5, null) is the last 5 rows");
    }

    static long removed(JsonNode plan) {
        long n = plan.path("Rows Removed by Filter").asLong(0);
        for (var child : plan.path("Plans")) n += removed(child);
        return n;
    }
    static boolean has(JsonNode plan, String type) {
        if (plan.path("Node Type").asString("").equals(type)) return true;
        for (var child : plan.path("Plans")) if (has(child, type)) return true;
        return false;
    }

    static JsonNode explain(Connection c, String sql, Row at, int limit) throws SQLException {
        int params = (int) sql.chars().filter(ch -> ch == '?').count();
        try (var ps = c.prepareStatement("explain (analyze, format json) " + sql)) {
            int i = 1;
            ps.setInt(i++, at.published());
            if (params == 4) ps.setInt(i++, at.published());   // the starter's OR form
            ps.setLong(i++, at.id());
            ps.setInt(i, limit);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return JsonMapper.builder().build().readTree(rs.getString(1)).get(0).get("Plan");
            }
        }
    }

    static void plans(Connection c) throws SQLException {
        Row deep = rowAt(c, 900_000);
        for (var e : new String[][]{{"FORWARD_SQL", Keyset.FORWARD_SQL}, {"BACKWARD_SQL", Keyset.BACKWARD_SQL}}) {
            String name = e[0], sql = e[1];
            if (sql == null || sql.isBlank()) { check(false, name + " is written"); continue; }
            check(sql.chars().filter(ch -> ch == '?').count() == 3, name + " takes 3 parameters: published, id, limit");
            var plan = explain(c, sql, deep, 21);
            long gone = removed(plan);
            check(!has(plan, "Seq Scan") && !has(plan, "Sort"), name + " at row 900,000 reads the (published, id) index in order: no Seq Scan, no Sort");
            check(gone <= 100, name + " at row 900,000 seeks instead of filtering: Rows Removed by Filter = " + gone + " (at most 100)");
        }
    }

    static void timing(Connection c) throws SQLException {
        Row deep = rowAt(c, 900_000);
        String cur = Keyset.encode(deep.published(), deep.id());
        long t = System.nanoTime();
        for (int i = 0; i < 5; i++) Keyset.offsetPage(c, 900_000, 20);
        long offset = (System.nanoTime() - t) / 5_000;
        t = System.nanoTime();
        for (int i = 0; i < 5; i++) Keyset.forward(c, 20, cur);
        long keyset = (System.nanoTime() - t) / 5_000;
        System.out.printf("info  20 rows at offset 900,000, mean of 5 from Java: OFFSET %d µs, keyset %d µs%n", offset, keyset);
    }
}
