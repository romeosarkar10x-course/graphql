package dev.shelfie.dynamic;

import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The m14_07.fields table, and the one table it describes (m14_07.books, for type Book). */
@Component
public class Metadata {
    public record FieldDef(String typeName, String name, String type, String column, String description) {}

    static final Set<String> TYPES = Set.of("STRING", "INT", "FLOAT", "BOOLEAN");
    static final String SQL_TYPE_STRING = "text";
    private final JdbcClient jdbc;

    Metadata(JdbcClient jdbc, SchemaSetup schema) {
        this.jdbc = jdbc;
    }

    public List<FieldDef> fields(String typeName) {
        return jdbc.sql("SELECT type_name, field_name AS name, graphql_type AS type, column_name AS column, description FROM m14_07.fields "
            + "WHERE type_name = ? ORDER BY position").param(typeName).query(FieldDef.class).list();
    }

    /** Changes whenever a row of the metadata (or a column of the table) changes: cheap to poll. */
    public String fingerprint() {
        return jdbc.sql("SELECT md5(coalesce((SELECT string_agg(concat_ws('|', type_name, field_name, graphql_type, column_name, description, position), ',' ORDER BY position) FROM m14_07.fields), '')"
            + " || coalesce((SELECT string_agg(column_name, ',' ORDER BY column_name) FROM information_schema.columns WHERE table_schema = 'm14_07' AND table_name = 'books'), ''))")
            .query(String.class).single();
    }

    public Set<String> columns() {
        return Set.copyOf(jdbc.sql("SELECT column_name FROM information_schema.columns WHERE table_schema = 'm14_07' AND table_name = 'books'")
            .query(String.class).list());
    }

    @Transactional
    public FieldDef add(String typeName, String name, String type, String description) {
        String column = name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
        String sqlType = switch (type) { case "INT" -> "integer"; case "FLOAT" -> "double precision"; case "BOOLEAN" -> "boolean"; default -> SQL_TYPE_STRING; };
        // The column name is derived from a validated GraphQL name (letters, digits, underscores), so it is safe to splice.
        jdbc.sql("ALTER TABLE m14_07.books ADD COLUMN IF NOT EXISTS " + column + " " + sqlType).update();
        jdbc.sql("INSERT INTO m14_07.fields (type_name, field_name, graphql_type, column_name, description) VALUES (?, ?, ?, ?, ?)")
            .params(typeName, name, type, column, description).update();
        return new FieldDef(typeName, name, type, column, description);
    }

    public FieldDef remove(String typeName, String name) {
        FieldDef def = fields(typeName).stream().filter(f -> f.name().equals(name)).findFirst().orElse(null);
        if (def != null) jdbc.sql("DELETE FROM m14_07.fields WHERE type_name = ? AND field_name = ?").params(typeName, name).update();
        return def;
    }

    public List<java.util.Map<String, Object>> books() {
        return jdbc.sql("SELECT * FROM m14_07.books ORDER BY id").query().listOfRows();
    }

    public java.util.Map<String, Object> book(String id) {
        return jdbc.sql("SELECT * FROM m14_07.books WHERE id = ?").param(id).query().listOfRows().stream().findFirst().orElse(null);
    }
}
