import java.sql.*;
public class Bench {
  public static void main(String[] a) throws Exception {
    try (var c = DriverManager.getConnection("jdbc:postgresql://postgres:5432/shelfie", "shelfie", "shelfie"); var s = c.createStatement()) {
      long t = System.nanoTime();
      s.execute("drop schema if exists probe cascade; create schema probe; set search_path = probe");
      s.execute("create table book (id bigint primary key, title text not null, published int not null)");
      s.execute("insert into book select g, 'Book ' || g, 1900 + g % 125 from generate_series(1, 1000000) g");
      s.execute("create index book_pub_id on book (published, id)");
      s.execute("analyze book");
      System.out.printf("load %d ms%n", (System.nanoTime() - t) / 1_000_000);
      for (String q : new String[]{
          "select id, title from book order by id limit 20 offset 0",
          "select id, title from book order by id limit 20 offset 900000",
          "select id, title from book where id > 900000 order by id limit 20",
          "select id from book order by published, id limit 20 offset 900000",
          "select id from book where (published, id) > (2019, 500000) order by published, id limit 20",
          "select count(*) from book"}) {
        System.out.println("== " + q);
        try (var r = s.executeQuery("explain (analyze, costs off, timing on) " + q)) { while (r.next()) System.out.println("  " + r.getString(1)); }
      }
    }
  }
}
