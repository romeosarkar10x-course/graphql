import java.sql.DriverManager;

/** Prints the row count of <schema>.book, or fails when the table is missing. */
public class Count {
    public static void main(String[] a) throws Exception {
        String url = System.getenv().getOrDefault("PGURL", "jdbc:postgresql://postgres:5432/shelfie");
        try (var c = DriverManager.getConnection(url, "shelfie", "shelfie"); var s = c.createStatement();
             var rs = s.executeQuery("select count(*) from " + a[0] + ".book")) {
            rs.next();
            System.out.println(rs.getLong(1));
        }
    }
}
