import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 8.1's experiment: scroll `pages` pages of `size` with offset paging while the list changes.
 *   insert: after each page, 3 new books arrive at the top (someone else is adding books)
 *   delete: after each page, 3 books you already saw are deleted
 * Then count what you saw twice and what you never saw.
 *
 *   java -cp "$(source /course/tools/testlib.sh; lib_cp)" Drift.java [insert|delete] [pages] [size] [url]
 */
public class Drift {
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final JsonMapper JSON = JsonMapper.builder().build();
    static String url = "http://localhost:4000/graphql";

    static JsonNode gql(String query, Map<String, Object> variables) throws Exception {
        var body = JSON.writeValueAsString(Map.of("query", query, "variables", variables));
        var req = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var res = JSON.readTree(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body());
        if (res.has("errors")) throw new IllegalStateException(res.get("errors").toString());
        return res.get("data");
    }

    static List<String> page(int page, int size) throws Exception {
        var ids = new ArrayList<String>();
        for (var item : gql("query($p: Int!, $s: Int!) { books(page: $p, size: $s) { items { id } } }", Map.of("p", page, "s", size)).at("/books/items"))
            ids.add(item.get("id").asString());
        return ids;
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "insert";
        int pages = args.length > 1 ? Integer.parseInt(args[1]) : 5, size = args.length > 2 ? Integer.parseInt(args[2]) : 20;
        if (args.length > 3) url = args[3];

        // The list as it was before we started, far enough to cover everything we will scroll past.
        var before = new ArrayList<String>();
        for (int p = 0; before.size() < pages * size + 3 * pages; p++) before.addAll(page(p, 50));

        var shown = new ArrayList<String>();
        var deleted = new HashSet<String>();
        for (int p = 0; p < pages; p++) {
            var ids = page(p, size);
            shown.addAll(ids);
            if (p == pages - 1) break;
            for (int i = 0; i < 3; i++) {
                if (mode.equals("insert")) gql("mutation($t: String!) { addBook(title: $t) { id } }", Map.of("t", "Drift " + p + "." + i));
                else { gql("mutation($id: ID!) { deleteBook(id: $id) }", Map.of("id", ids.get(i))); deleted.add(ids.get(i)); }
            }
        }
        var distinct = new LinkedHashSet<>(shown);
        int duplicates = shown.size() - distinct.size();
        // A gap: a book that existed before we started, is still there, sorts above the last book we saw, and never appeared.
        int last = before.indexOf(shown.getLast());
        int gaps = 0;
        for (int i = 0; i <= last; i++) if (!distinct.contains(before.get(i)) && !deleted.contains(before.get(i))) gaps++;
        System.out.printf("%s: pages %d, size %d, shown %d, distinct %d, duplicates %d, gaps %d%n",
            mode, pages, size, shown.size(), distinct.size(), duplicates, gaps);
    }
}
