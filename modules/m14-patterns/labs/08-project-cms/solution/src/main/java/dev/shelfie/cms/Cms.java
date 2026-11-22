package dev.shelfie.cms;

import dev.shelfie.cms.Store.Field;
import dev.shelfie.cms.Store.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * The rules. A definition must produce a valid schema that does not break what exists: names GraphQL accepts and
 * the API does not already use, references to types that exist, and no change that existing entries cannot meet
 * (a new required field, a field changing kind). Entry writes check what GraphQL's input types cannot: that a
 * reference points at an entry of the right type, and that nothing still points at an entry being deleted.
 */
@Service
public class Cms {
    public record UserError(List<String> field, String message) {}
    public record DefinePayload(Map<String, Object> contentType, List<UserError> userErrors) {}
    public record RemovePayload(String removed, List<UserError> userErrors) {}

    static final class Refused extends RuntimeException {
        Refused(String message) { super(message); }
    }

    static final Set<String> RESERVED = Set.of("Query", "Mutation", "Subscription", "ContentType", "ContentField", "FieldKind",
        "ContentTypeInput", "ContentFieldInput", "UserError", "DefinePayload", "RemovePayload", "String", "Int", "Float", "Boolean", "ID");
    static final String TYPE_NAME = "[A-Z][A-Za-z0-9]*", FIELD_NAME = "[a-z][A-Za-z0-9]*";

    private final Store store;
    private final CmsSchema schema;

    Cms(Store store, CmsSchema schema) {
        this.store = store;
        this.schema = schema;
        schema.attach(this);
    }

    @SuppressWarnings("unchecked")
    public DefinePayload define(Map<String, Object> input) {
        String name = (String) input.get("name"), plural = (String) input.get("plural");
        List<Field> fields = new ArrayList<>();
        for (Map<String, Object> f : (List<Map<String, Object>>) input.get("fields"))
            fields.add(new Field((String) f.get("name"), (String) f.get("kind"), (String) f.get("ref"),
                Boolean.TRUE.equals(f.get("required")), (String) f.get("description")));
        List<UserError> errors = new ArrayList<>();
        List<Type> types = store.types();
        Optional<Type> existing = types.stream().filter(t -> t.name().equals(name)).findFirst();

        if (!name.matches(TYPE_NAME) || RESERVED.contains(name) || name.endsWith("Input") || name.endsWith("Patch"))
            errors.add(new UserError(List.of("input", "name"), "'" + name + "' cannot be a content type: use PascalCase, not a name the API uses"));
        if (!plural.matches(TYPE_NAME) || plural.equals(name) || plural.equals("ContentTypes")
            || types.stream().anyMatch(t -> !t.name().equals(name) && (t.plural().equals(plural) || t.name().equals(plural))))
            errors.add(new UserError(List.of("input", "plural"), "'" + plural + "' cannot be the plural: PascalCase, unlike the name, not used by another type"));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            List<String> at = List.of("input", "fields", String.valueOf(i));
            if (!f.name().matches(FIELD_NAME) || f.name().equals("id") || f.name().equals("createdAt") || !seen.add(f.name()))
                errors.add(new UserError(at, "'" + f.name() + "' cannot be a field: camelCase, not id or createdAt, once per type"));
            if (f.kind().equals("REFERENCE") && (f.ref() == null || !(f.ref().equals(name) || types.stream().anyMatch(t -> t.name().equals(f.ref())))))
                errors.add(new UserError(at, "A REFERENCE needs ref, the name of this type or of a defined one; '" + f.ref() + "' is neither"));
            if (!f.kind().equals("REFERENCE") && f.ref() != null)
                errors.add(new UserError(at, "Only a REFERENCE has ref"));
        }
        if (existing.isPresent() && store.count(name) > 0) {
            Map<String, Field> before = new LinkedHashMap<>();
            existing.get().fields().forEach(f -> before.put(f.name(), f));
            for (int i = 0; i < fields.size(); i++) {
                Field f = fields.get(i), old = before.get(f.name());
                List<String> at = List.of("input", "fields", String.valueOf(i));
                if (old == null && f.required())
                    errors.add(new UserError(at, name + " has entries without '" + f.name() + "': a new field cannot be required"));
                else if (old != null && (!old.kind().equals(f.kind()) || (f.required() && !old.required())))
                    errors.add(new UserError(at, name + " has entries: '" + f.name() + "' cannot change kind or become required"));
            }
        }
        if (!errors.isEmpty()) return new DefinePayload(null, errors);
        store.define(new Type(name, plural, (String) input.get("description"), fields));
        schema.rebuild();
        return new DefinePayload(schema.view(store.types().stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow()), List.of());
    }

    public RemovePayload remove(String name) {
        List<Type> types = store.types();
        if (types.stream().noneMatch(t -> t.name().equals(name)))
            return new RemovePayload(null, List.of(new UserError(List.of("name"), "No content type " + name)));
        List<String> users = types.stream().filter(t -> !t.name().equals(name))
            .filter(t -> t.fields().stream().anyMatch(f -> name.equals(f.ref()))).map(Type::name).toList();
        if (!users.isEmpty())
            return new RemovePayload(null, List.of(new UserError(List.of("name"), name + " is referenced by " + String.join(", ", users))));
        store.remove(name);
        schema.rebuild();
        return new RemovePayload(name, List.of());
    }

    public Map<String, Object> create(Type t, Map<String, Object> input) {
        checkReferences(t, input);
        return store.entry(t.name(), store.create(t.name(), input));
    }

    public Map<String, Object> update(Type t, String id, Map<String, Object> patch) {
        for (Field f : t.fields())
            if (f.required() && patch.containsKey(f.name()) && patch.get(f.name()) == null) throw new Refused(t.name() + "." + f.name() + " is required: it cannot be set to null");
        checkReferences(t, patch);
        return store.update(t.name(), id, patch) ? store.entry(t.name(), id) : null;
    }

    public String delete(Type t, String id) {
        List<String> users = store.referencing(id);
        if (!users.isEmpty()) throw new Refused(id + " is referenced by " + String.join(", ", users));
        return store.delete(t.name(), id) ? id : null;
    }

    void checkReferences(Type t, Map<String, Object> values) {
        for (Field f : t.fields())
            if (f.kind().equals("REFERENCE") && values.get(f.name()) != null && store.entry(f.ref(), values.get(f.name()).toString()) == null)
                throw new Refused(t.name() + "." + f.name() + ": no " + f.ref() + " " + values.get(f.name()));
    }
}
