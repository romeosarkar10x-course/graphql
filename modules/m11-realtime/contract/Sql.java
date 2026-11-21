// Runs SQL against the course PostgreSQL (postgres:5432, database/user/password shelfie), outside any app.
//   bin/course java modules/m11-realtime/contract/Sql.java "INSERT INTO m11.messages (room_id, author, body) VALUES ('r1', 'psql', 'hi')"
// Prints the rows of a query (tab-separated) or the number of rows changed.
import java.sql.*;

public class Sql {
  public static void main(String[] a) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:postgresql://postgres:5432/shelfie", "shelfie", "shelfie");
         Statement s = c.createStatement()) {
      if (s.execute(a[0])) {
        ResultSet r = s.getResultSet();
        int n = r.getMetaData().getColumnCount();
        while (r.next()) {
          StringBuilder b = new StringBuilder();
          for (int i = 1; i <= n; i++) b.append(i > 1 ? "\t" : "").append(r.getString(i));
          System.out.println(b);
        }
      } else System.out.println(s.getUpdateCount());
    }
  }
}
