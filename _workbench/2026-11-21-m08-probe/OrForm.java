import java.sql.*;
public class OrForm {
  public static void main(String[] a) throws Exception {
    try (var c = DriverManager.getConnection("jdbc:postgresql://postgres:5432/shelfie?currentSchema=probe", "shelfie", "shelfie"); var s = c.createStatement()) {
      for (String q : new String[]{
          "select id from book where published > 2019 or published = 2019 and id > 500000 order by published, id limit 20",
          "select id from book where (published, id) > (2019, 500000) order by published, id limit 20",
          "select id from book where published > 1950 or published = 1950 and id > 50000 order by published, id limit 20",
          "select id from book where published >= 2019 and (published > 2019 or published = 2019 and id > 500000) order by published, id limit 20"}) {
        System.out.println("== " + q);
        try (var r = s.executeQuery("explain (analyze, costs off) " + q)) { while (r.next()) System.out.println("  " + r.getString(1)); }
      }
    }
  }
}
