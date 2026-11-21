import java.sql.DriverManager;

/**
 * Prints EXPLAIN ANALYZE's execution time and rows read for OFFSET, the row-value keyset query and the
 * OR form, at offsets 0, 10,000, 100,000 and 900,000. Run it from bin/course shell:
 *   java -cp "$(source /course/tools/testlib.sh; lib_cp)" Explain.java
 * Times vary from run to run and machine to machine; the rows read do not.
 */
public class Explain {
    public static void main(String[] args) throws Exception {
        String url = System.getenv().getOrDefault("PGURL", "jdbc:postgresql://postgres:5432/shelfie") + "?currentSchema=m08_02";
        try (var c = DriverManager.getConnection(url, "shelfie", "shelfie"); var s = c.createStatement()) {
            for (int off : new int[]{0, 10_000, 100_000, 900_000}) {
                int p; long id;
                try (var r = s.executeQuery("select published, id from book order by published, id offset " + Math.max(0, off - 1) + " limit 1")) {
                    r.next(); p = r.getInt(1); id = r.getLong(2);
                }
                String[][] queries = {
                    {"OFFSET", "select id, published from book order by published, id limit 20 offset " + off},
                    {"ROW", "select id, published from book where (published, id) > (" + p + ", " + id + ") order by published, id limit 20"},
                    {"OR", "select id, published from book where published > " + p + " or published = " + p + " and id > " + id + " order by published, id limit 20"}};
                for (var q : queries) {
                    String time = "", read = "", removed = "";
                    try (var r = s.executeQuery("explain (analyze) " + q[1])) {
                        while (r.next()) {
                            var line = r.getString(1);
                            if (line.contains("Execution Time")) time = line.trim();
                            if (line.contains("Rows Removed")) removed = " | " + line.trim();
                            if (read.isEmpty() && line.contains("Scan")) read = line.replaceAll(".*actual time=\\S+ (rows=\\S+).*", "$1");
                        }
                    }
                    System.out.printf("offset %7d | %-6s | %s | scan %s%s%n", off, q[0], time, read, removed);
                }
            }
        }
    }
}
