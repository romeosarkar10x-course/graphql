package dev.shelfie.cms;

import dev.shelfie.cms.Store.Field;
import dev.shelfie.cms.Store.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Stores definitions as they come, checks nothing, and the API never changes.
 *
 * TODO the API generated from the definitions, at run time, for every content type: a GraphQL type, an input and a
 * patch, a query for one entry and one for a list, create/update/delete mutations (see README.md for the names).
 * Swap the schema when a definition changes (lab 14.7). And the rules that keep the generated schema valid.
 */
@Controller
public class CmsController {
    public record UserError(List<String> field, String message) {}
    public record DefinePayload(Map<String, Object> contentType, List<UserError> userErrors) {}
    public record RemovePayload(String removed, List<UserError> userErrors) {}

    private final Store store;

    CmsController(Store store) {
        this.store = store;
    }

    @QueryMapping public List<Map<String, Object>> contentTypes() {
        return store.types().stream().map(this::view).toList();
    }

    @SuppressWarnings("unchecked")
    @MutationMapping public DefinePayload defineContentType(@Argument Map<String, Object> input) {
        List<Field> fields = new ArrayList<>();
        for (Map<String, Object> f : (List<Map<String, Object>>) input.get("fields"))
            fields.add(new Field((String) f.get("name"), (String) f.get("kind"), (String) f.get("ref"),
                Boolean.TRUE.equals(f.get("required")), (String) f.get("description")));
        Type t = new Type((String) input.get("name"), (String) input.get("plural"), (String) input.get("description"), fields);
        store.define(t);
        return new DefinePayload(view(t), List.of());
    }

    @MutationMapping public RemovePayload removeContentType(@Argument String name) {
        store.remove(name);
        return new RemovePayload(name, List.of());
    }

    Map<String, Object> view(Type t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", t.name());
        m.put("plural", t.plural());
        m.put("description", t.description());
        m.put("fields", t.fields());
        m.put("entryCount", store.count(t.name()));
        return m;
    }
}
