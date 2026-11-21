import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.util.*;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 8.4 harness: 300 random filter/order/first combinations (seeded) sent to the app over HTTP,
 * compared with an independent oracle: Java over every row for the structured fields, PostgreSQL itself
 * for full-text matching and ranks. Plus the error rules.
 */
public class FilterOracle {
    record Row(long id, String title, String author, String genre, int published, BigDecimal rating) {}

    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final String[] GENRES = {"FANTASY", "MYSTERY", "SCIENCE_FICTION", "ROMANCE", "HISTORY", "POETRY", "THRILLER", "MEMOIR"};
    static final String[] AUTHORS = {"Mei Mensah", "Tomas Costa", "Omar Lindqvist", "Ines Tanaka", "Grace Kim", "Nobody Atall"};
    static final String[] SEARCHES = {"dragon", "lisbon", "\"stolen map\"", "lighthouse -oslo", "garden or river", "murder kyoto",
        "winter", "circus seoul", "\"first love\" -dublin", "archive"};
    static String url;
    static int fails = 0, oks = 0;

    public static void main(String[] args) throws Exception {
        url = args[0];
        String pg = System.getenv().getOrDefault("PGURL", "jdbc:postgresql://postgres:5432/shelfie") + "?currentSchema=m08_04";
        var rows = new ArrayList<Row>();
        try (var c = DriverManager.getConnection(pg, "shelfie", "shelfie")) {
            try (var s = c.createStatement(); var rs = s.executeQuery("select id, title, author, genre, published, rating from book")) {
                while (rs.next()) rows.add(new Row(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getBigDecimal(6)));
            }
            var rnd = new Random(808);
            for (int n = 0; n < 300; n++) runCase(c, rows, rnd, n);
        }
        errors();
        System.out.printf("%d of %d cases match the oracle%n", oks, oks + fails);
        System.exit(fails == 0 ? 0 : 1);
    }

    static void runCase(java.sql.Connection c, List<Row> rows, Random rnd, int n) throws Exception {
        var filter = new LinkedHashMap<String, Object>();
        if (rnd.nextBoolean()) {
            var g = new ArrayList<>(List.of(GENRES)); Collections.shuffle(g, rnd);
            filter.put("genres", g.subList(0, rnd.nextInt(4)));
        }
        Integer from = rnd.nextBoolean() ? 1890 + rnd.nextInt(141) : null, to = rnd.nextBoolean() ? 1890 + rnd.nextInt(141) : null;
        if (from != null && to != null && from > to) { int t = from; from = to; to = t; }
        if (from != null) filter.put("publishedFrom", from);
        if (to != null) filter.put("publishedTo", to);
        if (rnd.nextInt(10) < 4) filter.put("minRating", (10 + rnd.nextInt(41)) / 10.0);
        if (rnd.nextInt(10) < 2) filter.put("author", AUTHORS[rnd.nextInt(AUTHORS.length)]);
        String search = rnd.nextInt(10) < 4 ? SEARCHES[rnd.nextInt(SEARCHES.length)] : null;
        if (search != null) filter.put("search", search);
        String field = search != null && rnd.nextBoolean() ? "RELEVANCE" : List.of("TITLE", "PUBLISHED", "RATING").get(rnd.nextInt(3));
        String dir = rnd.nextBoolean() ? "ASC" : "DESC";
        int first = rnd.nextInt(10) == 0 ? 0 : 1 + rnd.nextInt(100);

        // The oracle
        Map<Long, Float> ranks = null;
        if (search != null) {
            ranks = new HashMap<>();
            try (var ps = c.prepareStatement("select id, ts_rank(to_tsvector('english', title || ' ' || blurb), websearch_to_tsquery('english', ?)) from book "
                + "where to_tsvector('english', title || ' ' || blurb) @@ websearch_to_tsquery('english', ?)")) {
                ps.setString(1, search); ps.setString(2, search);
                try (var rs = ps.executeQuery()) { while (rs.next()) ranks.put(rs.getLong(1), rs.getFloat(2)); }
            }
        }
        final Map<Long, Float> r = ranks; final Integer f0 = from, t0 = to;
        @SuppressWarnings("unchecked") var genres = (List<String>) filter.get("genres");
        var minRating = filter.containsKey("minRating") ? BigDecimal.valueOf((Double) filter.get("minRating")) : null;
        var author = (String) filter.get("author");
        var matching = rows.stream().filter(b -> (genres == null || genres.contains(b.genre())) && (f0 == null || b.published() >= f0)
            && (t0 == null || b.published() <= t0) && (minRating == null || b.rating().compareTo(minRating) >= 0)
            && (author == null || b.author().equals(author)) && (r == null || r.containsKey(b.id()))).toList();
        Comparator<Row> key = switch (field) {
            case "TITLE" -> Comparator.comparing(Row::title);
            case "PUBLISHED" -> Comparator.comparingInt(Row::published);
            case "RATING" -> Comparator.comparing(Row::rating);
            default -> Comparator.comparing((Row b) -> r.get(b.id())).reversed();
        };
        if (dir.equals("DESC") && !field.equals("RELEVANCE")) key = key.reversed();
        var expected = matching.stream().sorted(key.thenComparingLong(Row::id)).limit(first).map(b -> String.valueOf(b.id())).toList();

        var order = Map.of("field", field, "direction", dir);
        var res = gql("query($f: BookFilter, $o: BookOrder, $n: Int) { books(filter: $f, orderBy: $o, first: $n) { items { id } totalCount } }",
            Map.of("f", filter, "o", order, "n", first));
        String what = "case " + n + " " + JSON.writeValueAsString(Map.of("filter", filter, "orderBy", order, "first", first));
        if (res.has("errors")) { bad(what + ": unexpected error " + res.get("errors").get(0).path("message").asString()); return; }
        var got = new ArrayList<String>();
        for (var item : res.at("/data/books/items")) got.add(item.get("id").asString());
        long total = res.at("/data/books/totalCount").asLong(-1);
        if (!got.equals(expected)) {
            int i = 0; while (i < Math.min(got.size(), expected.size()) && got.get(i).equals(expected.get(i))) i++;
            bad(what + ": items differ at position " + i + " (got " + got.size() + " items, expected " + expected.size() + ")");
        } else if (total != matching.size()) bad(what + ": totalCount " + total + ", expected " + matching.size());
        else oks++;
    }

    static void errors() throws Exception {
        String[][] cases = {
            {"{ books(filter: {publishedFrom: 2000, publishedTo: 1990}) { totalCount } }", "publishedFrom after publishedTo"},
            {"{ books(orderBy: {field: RELEVANCE}) { totalCount } }", "RELEVANCE without search"},
            {"{ books(first: 101) { totalCount } }", "first: 101"},
            {"{ books(first: -1) { totalCount } }", "first: -1"},
            {"{ books(filter: {minRating: 0.5}) { totalCount } }", "minRating 0.5"},
            {"{ books(filter: {search: \"   \"}) { totalCount } }", "a blank search"}};
        for (var e : cases) {
            var res = gql(e[0], Map.of());
            String cls = res.at("/errors/0/extensions/classification").asString("");
            if (cls.equals("BAD_REQUEST")) oks++; else bad(e[1] + ": expected a BAD_REQUEST error, got " + (res.has("errors") ? cls : "no error"));
        }
        var empty = gql("{ books(filter: {genres: []}) { items { id } totalCount } }", Map.of());
        if (empty.at("/data/books/totalCount").asLong(-1) == 0 && !empty.has("errors")) oks++; else bad("genres: [] matches nothing (not everything, not an error)");
    }

    static void bad(String m) { fails++; if (fails <= 8) System.out.println("FAIL  " + m); }

    static JsonNode gql(String query, Map<String, Object> variables) throws Exception {
        var body = JSON.writeValueAsString(Map.of("query", query, "variables", variables));
        var req = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return JSON.readTree(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body());
    }
}
