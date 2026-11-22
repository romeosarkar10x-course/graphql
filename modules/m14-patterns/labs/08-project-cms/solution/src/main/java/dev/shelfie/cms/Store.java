package dev.shelfie.cms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Content types, their fields, and entries (a JSON document per entry). */
@Component
public class Store {
    public record Field(String name, String kind, String ref, boolean required, String description) {}
    public record Type(String name, String plural, String description, List<Field> fields) {}

    private final JdbcClient jdbc;
    private final JsonMapper json;

    Store(JdbcClient jdbc, JsonMapper json, SchemaSetup schema) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<Type> types() {
        List<Type> out = new ArrayList<>();
        for (var row : jdbc.sql("SELECT name, plural, description FROM m14_08.content_types ORDER BY position").query().listOfRows()) {
            String name = (String) row.get("name");
            out.add(new Type(name, (String) row.get("plural"), (String) row.get("description"), fields(name)));
        }
        return out;
    }

    List<Field> fields(String type) {
        return jdbc.sql("SELECT name, kind, ref, required, description FROM m14_08.content_fields WHERE type_name = ? ORDER BY position")
            .param(type).query(Field.class).list();
    }

    @Transactional
    public void define(Type t) {
        jdbc.sql("INSERT INTO m14_08.content_types (name, plural, description) VALUES (?, ?, ?) "
            + "ON CONFLICT (name) DO UPDATE SET plural = EXCLUDED.plural, description = EXCLUDED.description")
            .params(t.name(), t.plural(), t.description()).update();
        jdbc.sql("DELETE FROM m14_08.content_fields WHERE type_name = ?").param(t.name()).update();
        for (Field f : t.fields())
            jdbc.sql("INSERT INTO m14_08.content_fields (type_name, name, kind, ref, required, description) VALUES (?, ?, ?, ?, ?, ?)")
                .params(t.name(), f.name(), f.kind(), f.ref(), f.required(), f.description()).update();
    }

    @Transactional
    public void remove(String type) {
        jdbc.sql("DELETE FROM m14_08.entries WHERE type_name = ?").param(type).update();
        jdbc.sql("DELETE FROM m14_08.content_types WHERE name = ?").param(type).update();
    }

    public int count(String type) {
        return jdbc.sql("SELECT count(*) FROM m14_08.entries WHERE type_name = ?").param(type).query(Integer.class).single();
    }

    public Map<String, Object> entry(String type, String id) {
        return jdbc.sql("SELECT id, created_at, data::text FROM m14_08.entries WHERE id = ? AND type_name = ?").params(id, type)
            .query((rs, n) -> row(rs.getString(1), rs.getTimestamp(2), rs.getString(3))).optional().orElse(null);
    }

    public List<Map<String, Object>> entries(String type, int first) {
        return jdbc.sql("SELECT id, created_at, data::text FROM m14_08.entries WHERE type_name = ? ORDER BY created_at, id LIMIT ?")
            .params(type, Math.max(0, first)).query((rs, n) -> row(rs.getString(1), rs.getTimestamp(2), rs.getString(3))).list();
    }

    /** Ids of entries whose JSON points at this id. */
    public List<String> referencing(String id) {
        return jdbc.sql("SELECT e.id FROM m14_08.entries e JOIN m14_08.content_fields f ON f.type_name = e.type_name AND f.kind = 'REFERENCE' "
            + "WHERE e.data ->> f.name = ? ORDER BY e.id").param(id).query(String.class).list();
    }

    public String create(String type, Map<String, Object> data) {
        String id = type.toLowerCase() + "-" + jdbc.sql("SELECT nextval('m14_08.entry_ids')").query(Long.class).single();
        jdbc.sql("INSERT INTO m14_08.entries (id, type_name, data) VALUES (?, ?, ?::jsonb)").params(id, type, json.writeValueAsString(data)).update();
        return id;
    }

    public boolean update(String type, String id, Map<String, Object> patch) {
        return jdbc.sql("UPDATE m14_08.entries SET data = data || ?::jsonb WHERE id = ? AND type_name = ?")
            .params(json.writeValueAsString(patch), id, type).update() > 0;
    }

    public boolean delete(String type, String id) {
        return jdbc.sql("DELETE FROM m14_08.entries WHERE id = ? AND type_name = ?").params(id, type).update() > 0;
    }

    Map<String, Object> row(String id, java.sql.Timestamp at, String data) {
        Map<String, Object> m = new LinkedHashMap<>(json.readValue(data, new TypeReference<Map<String, Object>>() {}));
        m.put("id", id);
        m.put("createdAt", at.toInstant().toString());
        return m;
    }
}
