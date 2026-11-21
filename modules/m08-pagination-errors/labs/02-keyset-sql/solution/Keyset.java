import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/** Lab 8.2: keyset (seek) paging over book, ordered by (published, id). The harness calls these. */
public final class Keyset {
    private Keyset() {}

    /** One page: ids in (published, id) order, plus what a Relay PageInfo needs. */
    public record Page(List<Long> ids, String startCursor, String endCursor, boolean hasNextPage, boolean hasPreviousPage) {}

    /** A decoded cursor: the sort key of one row. */
    public record Position(int published, long id) {}

    // A row-value comparison is one range condition on the (published, id) index: PostgreSQL seeks to
    // the position and reads `limit` rows. Parameters: published, id, limit.
    public static final String FORWARD_SQL =
        "select id, published from book where (published, id) > (?, ?) order by published, id limit ?";
    public static final String BACKWARD_SQL =
        "select id, published from book where (published, id) < (?, ?) order by published desc, id desc limit ?";

    /** The baseline: OFFSET reads and throws away `offset` rows first. Given, not graded. */
    public static List<Long> offsetPage(Connection c, int offset, int limit) throws SQLException {
        try (var ps = c.prepareStatement("select id from book order by published, id limit ? offset ?")) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            var ids = new ArrayList<Long>();
            try (var rs = ps.executeQuery()) { while (rs.next()) ids.add(rs.getLong(1)); }
            return ids;
        }
    }

    public static String encode(int published, long id) {
        var raw = "v1:" + published + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** @throws IllegalArgumentException for anything encode did not produce */
    public static Position decode(String cursor) {
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("not a cursor: " + cursor);
        }
        var parts = raw.split(":", -1);
        if (parts.length != 3 || !parts[0].equals("v1")) throw new IllegalArgumentException("not a cursor: " + cursor);
        try {
            return new Position(Integer.parseInt(parts[1]), Long.parseLong(parts[2]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a cursor: " + cursor);
        }
    }

    /** first rows after the cursor (null: from the start). */
    public static Page forward(Connection c, int first, String after) throws SQLException {
        var from = after == null ? new Position(Integer.MIN_VALUE, Long.MIN_VALUE) : decode(after);
        var rows = fetch(c, FORWARD_SQL, from, first + 1);       // one extra row answers hasNextPage
        boolean more = rows.size() > first;
        if (more) rows = rows.subList(0, first);
        return page(rows, more, after != null);
    }

    /** last rows before the cursor (null: up to the end), still in ascending order. */
    public static Page backward(Connection c, int last, String before) throws SQLException {
        var to = before == null ? new Position(Integer.MAX_VALUE, Long.MAX_VALUE) : decode(before);
        var rows = fetch(c, BACKWARD_SQL, to, last + 1);
        boolean more = rows.size() > last;
        if (more) rows = rows.subList(0, last);
        rows = new ArrayList<>(rows);
        Collections.reverse(rows);                                // fetched descending, shown ascending
        return page(rows, before != null, more);
    }

    private static List<Position> fetch(Connection c, String sql, Position p, int limit) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, p.published());
            ps.setLong(2, p.id());
            ps.setInt(3, limit);
            var out = new ArrayList<Position>();
            try (var rs = ps.executeQuery()) { while (rs.next()) out.add(new Position(rs.getInt(2), rs.getLong(1))); }
            return out;
        }
    }

    private static Page page(List<Position> rows, boolean hasNext, boolean hasPrevious) {
        var ids = rows.stream().map(Position::id).toList();
        String start = rows.isEmpty() ? null : encode(rows.getFirst().published(), rows.getFirst().id());
        String end = rows.isEmpty() ? null : encode(rows.getLast().published(), rows.getLast().id());
        return new Page(ids, start, end, hasNext, hasPrevious);
    }
}
