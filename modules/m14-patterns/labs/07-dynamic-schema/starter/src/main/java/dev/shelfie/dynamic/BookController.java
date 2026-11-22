package dev.shelfie.dynamic;

import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Serves the fixed schema in graphql/shelfie.graphqls. A new column needs a new schema line, a new mapping, a new
 * build and a restart.
 *
 * TODO the public schema built at run time from m14_07.fields (Metadata has the queries), swapped when the
 * metadata changes, with no restart; and a second endpoint, /admin/graphql, with admin/admin.graphqls.
 */
@Controller
public class BookController {
    private final Metadata metadata;

    BookController(Metadata metadata) {
        this.metadata = metadata;
    }

    @QueryMapping public List<Map<String, Object>> books() { return metadata.books(); }
    @QueryMapping public Map<String, Object> book(@Argument String id) { return metadata.book(id); }
    @SchemaMapping(typeName = "Book") public Object publishedYear(Map<String, Object> book) { return book.get("published_year"); }
}
