import java.sql.*;
public class Deep {
  public static void main(String[] a) throws Exception {
    try (var c = DriverManager.getConnection("jdbc:postgresql://postgres:5432/shelfie?currentSchema=m08_02", "shelfie", "shelfie"); var s = c.createStatement()) {
      for (int off : new int[]{0, 10_000, 100_000, 900_000}) {
        int p; long id;
        try (var r = s.executeQuery("select published, id from book order by published, id offset " + Math.max(0, off - 1) + " limit 1")) { r.next(); p = r.getInt(1); id = r.getLong(2); }
        for (String q : new String[]{
            "select id, published from book order by published, id limit 20 offset " + off,
            "select id, published from book where (published, id) > (" + p + ", " + id + ") order by published, id limit 20",
            "select id, published from book where published > " + p + " or published = " + p + " and id > " + id + " order by published, id limit 20"}) {
          String exec = "", rows = "", removed = "-";
          try (var r = s.executeQuery("explain (analyze) " + q)) { while (r.next()) { var l = r.getString(1);
            if (l.contains("Execution Time")) exec = l.trim(); if (l.contains("Rows Removed")) removed = l.trim(); if (rows.isEmpty() && l.contains("Scan")) rows = l.replaceAll(".*actual time=[^ ]+ (rows=[^ ]+).*", "$1"); } }
          System.out.printf("offset %7d | %-6s | %s | scan %s | %s%n", off, q.contains("offset") ? "OFFSET" : q.contains("(published, id) >") ? "ROW" : "OR", exec, rows, removed);
        }
      }
    }
  }
}
