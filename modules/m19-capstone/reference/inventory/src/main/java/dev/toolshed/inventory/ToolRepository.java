package dev.toolshed.inventory;

import dev.toolshed.common.BadInput;
import dev.toolshed.common.Cursors;
import dev.toolshed.inventory.Model.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL for the inventory. Every list read is one statement, whatever the number of ids. */
@Repository
class ToolRepository {
    private static final String TOOL_COLUMNS =
            "id, barcode, name, condition, category_id, branch_id, added_at, kind, volts, cordless, weight_grams";
    private final JdbcClient db;

    ToolRepository(JdbcClient db) {
        this.db = db;
    }

    Optional<Tool> byId(String id) {
        return db.sql("SELECT " + TOOL_COLUMNS + " FROM toolshed_inventory.tool WHERE id = ?").param(id).query(ToolRepository::tool).optional();
    }

    Optional<Tool> byBarcode(String barcode) {
        return db.sql("SELECT " + TOOL_COLUMNS + " FROM toolshed_inventory.tool WHERE barcode = ?").param(barcode).query(ToolRepository::tool).optional();
    }

    List<Tool> byIds(Collection<String> ids) {
        return db.sql("SELECT " + TOOL_COLUMNS + " FROM toolshed_inventory.tool WHERE id = ANY(?)").param(ids.toArray(String[]::new)).query(ToolRepository::tool).list();
    }

    /** One page by keyset: {@code first + 1} rows, so hasNextPage needs no count. */
    ToolConnection page(ToolFilter filter, ToolSort sort, int first, String after) {
        Where w = where(filter);
        if (after != null) {
            // the cursor names its sort order: a NEWEST cursor must not be read as a name under NAME_ASC
            List<String> key = Cursors.decode(after, 3);
            if (!key.get(0).equals(sort.name())) throw new BadInput("after", "this cursor is not valid for this sort order");
            switch (sort) {
                case NAME_ASC -> w.and("(lower(name), id) > (?, ?)", key.get(1), key.get(2));
                case NAME_DESC -> w.and("(lower(name), id) < (?, ?)", key.get(1), key.get(2));
                case NEWEST -> w.and("(added_at, id) < (?::timestamptz, ?)", key.get(1), key.get(2));
            }
        }
        String order = switch (sort) {
            case NAME_ASC -> "lower(name), id";
            case NAME_DESC -> "lower(name) DESC, id DESC";
            case NEWEST -> "added_at DESC, id DESC";
        };
        List<Tool> rows = db.sql("SELECT " + TOOL_COLUMNS + " FROM toolshed_inventory.tool" + w.sql() + " ORDER BY " + order + " LIMIT ?")
                .params(w.withParam(first + 1)).query(ToolRepository::tool).list();
        boolean more = rows.size() > first;
        List<ToolEdge> edges = rows.stream().limit(first).map(t -> new ToolEdge(cursor(t, sort), t)).toList();
        return new ToolConnection(edges, new PageInfo(more, edges.isEmpty() ? null : edges.getLast().cursor()), filter);
    }

    int count(ToolFilter filter) {
        Where w = where(filter);
        return db.sql("SELECT count(*) FROM toolshed_inventory.tool" + w.sql()).params(w.params).query(Integer.class).single();
    }

    List<Category> categories() {
        return db.sql("SELECT id, name FROM toolshed_inventory.category ORDER BY name").query(Category.class).list();
    }

    List<Category> categoriesByIds(Collection<String> ids) {
        return db.sql("SELECT id, name FROM toolshed_inventory.category WHERE id = ANY(?)").param(ids.toArray(String[]::new)).query(Category.class).list();
    }

    Map<String, Integer> toolCounts(Collection<String> categoryIds) {
        Map<String, Integer> counts = new HashMap<>();
        db.sql("SELECT category_id, count(*) FROM toolshed_inventory.tool WHERE category_id = ANY(?) GROUP BY category_id")
                .param(categoryIds.toArray(String[]::new))
                .query((rs, i) -> counts.put(rs.getString(1), rs.getInt(2))).list();
        return counts;
    }

    List<Branch> branches() {
        return db.sql("SELECT id, name, address, opening_hours FROM toolshed_inventory.branch ORDER BY name").query(Branch.class).list();
    }

    List<Branch> branchesByIds(Collection<String> ids) {
        return db.sql("SELECT id, name, address, opening_hours FROM toolshed_inventory.branch WHERE id = ANY(?)").param(ids.toArray(String[]::new)).query(Branch.class).list();
    }

    /** Tools, then categories, then branches, each by name; one statement. */
    List<SearchResult> search(String text, int first) {
        String like = "%" + text.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        List<String[]> hits = db.sql("""
                SELECT kind, id FROM (
                  SELECT 1 AS rank, 'tool' AS kind, id, name FROM toolshed_inventory.tool WHERE lower(name) LIKE ?
                  UNION ALL SELECT 2, 'category', id, name FROM toolshed_inventory.category WHERE lower(name) LIKE ?
                  UNION ALL SELECT 3, 'branch', id, name FROM toolshed_inventory.branch WHERE lower(name) LIKE ?
                ) hits ORDER BY rank, lower(name), id LIMIT ?""")
                .params(like, like, like, first).query((rs, i) -> new String[] {rs.getString(1), rs.getString(2)}).list();
        Map<String, List<String>> ids = new HashMap<>();
        for (String[] h : hits) ids.computeIfAbsent(h[0], k -> new ArrayList<>()).add(h[1]);
        Map<String, SearchResult> found = new HashMap<>();
        if (ids.containsKey("tool")) byIds(ids.get("tool")).forEach(t -> found.put("tool:" + t.id(), t));
        if (ids.containsKey("category")) categoriesByIds(ids.get("category")).forEach(c -> found.put("category:" + c.id(), c));
        if (ids.containsKey("branch")) branchesByIds(ids.get("branch")).forEach(b -> found.put("branch:" + b.id(), b));
        return hits.stream().map(h -> found.get(h[0] + ":" + h[1])).toList();
    }

    private static String cursor(Tool t, ToolSort sort) {
        return sort == ToolSort.NEWEST ? Cursors.encode(sort.name(), t.addedAt().toString(), t.id())
                : Cursors.encode(sort.name(), t.name().toLowerCase(Locale.ROOT), t.id());
    }

    private static Where where(ToolFilter f) {
        Where w = new Where();
        if (f.categoryId() != null) w.and("category_id = ?", f.categoryId());
        if (f.branchId() != null) w.and("branch_id = ?", f.branchId());
        if (f.condition() != null && !f.condition().isEmpty())
            w.and("condition = ANY(?)", (Object) f.condition().stream().map(Enum::name).toArray(String[]::new));
        if (f.nameContains() != null && !f.nameContains().isBlank())
            w.and("strpos(lower(name), ?) > 0", f.nameContains().toLowerCase(Locale.ROOT));
        if (f.powered() != null) w.and("kind = ?", f.powered() ? "POWER" : "HAND");
        return w;
    }

    private static Tool tool(ResultSet rs, int row) throws SQLException {
        ToolSpecs specs = rs.getString("kind").equals("POWER")
                ? new PowerSpecs(rs.getInt("volts"), rs.getBoolean("cordless"))
                : new HandSpecs(rs.getInt("weight_grams"));
        return new Tool(rs.getString("id"), rs.getString("barcode"), rs.getString("name"),
                Condition.valueOf(rs.getString("condition")), rs.getString("category_id"), rs.getString("branch_id"),
                rs.getObject("added_at", OffsetDateTime.class), specs);
    }

    /** A WHERE clause built from optional parts, with its parameters in order. */
    private static final class Where {
        final List<String> parts = new ArrayList<>();
        final List<Object> params = new ArrayList<>();

        void and(String sql, Object... values) {
            parts.add(sql);
            params.addAll(Arrays.asList(values));
        }

        String sql() {
            return parts.isEmpty() ? "" : " WHERE " + String.join(" AND ", parts);
        }

        List<Object> withParam(Object last) {
            List<Object> all = new ArrayList<>(params);
            all.add(last);
            return all;
        }
    }
}
