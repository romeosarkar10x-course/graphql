package probe;
import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.pagination.CursorStrategy;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

@SpringBootApplication public class App { public static void main(String[] a) { SpringApplication.run(App.class, a); }
  @Bean org.springframework.graphql.data.pagination.EncodingCursorStrategy<ScrollPosition> cursors() { return CursorStrategy.withEncoder(new Cursors(), org.springframework.graphql.data.pagination.CursorEncoder.base64()); } }
class Cursors implements CursorStrategy<ScrollPosition> {
  public boolean supports(Class<?> t) { return ScrollPosition.class.isAssignableFrom(t); }
  public String toCursor(ScrollPosition p) { var k = ((KeysetScrollPosition) p).getKeys();
    return "v1:" + k.get("published") + ":" + k.get("id"); }
  public ScrollPosition fromCursor(String c) { var s = c.split(":");
    return ScrollPosition.forward(Map.of("published", Integer.parseInt(s[1]), "id", Long.parseLong(s[2]))); }
}
@Entity @Table(name = "book") class Book { @Id Long id; String title; Integer published;
  public Long getId() { return id; } public String getTitle() { return title; } public Integer getPublished() { return published; } }
interface Books extends Repository<Book, Long> {
  @Query("select b from Book b where (b.published, b.id) > (:p, :id) order by b.published, b.id") List<Book> after(@Param("p") int p, @Param("id") long id, Limit l);
  @Query("select b from Book b where (b.published, b.id) < (:p, :id) order by b.published desc, b.id desc") List<Book> before(@Param("p") int p, @Param("id") long id, Limit l);
}
@Controller class C {
  final Books books; C(Books b) { books = b; }
  @QueryMapping Window<Book> books(ScrollSubrange sub) {
    int n = sub.count().orElse(10);
    var pos = sub.position().map(p -> ((KeysetScrollPosition) p).getKeys()).orElse(null);
    System.out.println("POSITION " + sub.position() + " forward=" + sub.forward());
    List<Book> rows = sub.forward()
        ? books.after(pos == null ? Integer.MIN_VALUE : (Integer) pos.get("published"), pos == null ? Long.MIN_VALUE : (Long) pos.get("id"), Limit.of(n + 1))
        : books.before(pos == null ? Integer.MAX_VALUE : (Integer) pos.get("published"), pos == null ? Long.MAX_VALUE : (Long) pos.get("id"), Limit.of(n + 1));
    boolean more = rows.size() > n;
    var page = new ArrayList<>(rows.subList(0, Math.min(n, rows.size())));
    if (!sub.forward()) Collections.reverse(page);
    var dir = sub.forward() ? KeysetScrollPosition.Direction.FORWARD : KeysetScrollPosition.Direction.BACKWARD;
    return Window.from(page, i -> ScrollPosition.of(Map.of("published", page.get(i).getPublished(), "id", page.get(i).getId()), dir), more);
  }
}
