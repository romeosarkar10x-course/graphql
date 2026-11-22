// Lab 14.3: checks the database itself, with no app in between. Run while the app is up (it creates m14_03).
// Prints one line per check: "ok <text>" or "FAIL <text>: <why>". Exit 1 if any failed.
import java.sql.*;
import java.util.*;

public class RlsCheck {
  static int failures = 0;

  public static void main(String[] a) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:postgresql://postgres:5432/shelfie", "shelfie", "shelfie")) {
      for (String table : List.of("clubs", "members")) {
        try (PreparedStatement s = c.prepareStatement("SELECT c.relrowsecurity, c.relforcerowsecurity, "
            + "(SELECT count(*) FROM pg_policies p WHERE p.schemaname = 'm14_03' AND p.tablename = c.relname) "
            + "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'm14_03' AND c.relname = ?")) {
          s.setString(1, table);
          ResultSet r = s.executeQuery();
          if (!r.next()) { check(false, "m14_03." + table + " has row-level security", "no such table"); continue; }
          check(r.getBoolean(1) && r.getBoolean(2) && r.getInt(3) > 0, "m14_03." + table + " has row-level security, forced, with a policy",
              "enabled=" + r.getBoolean(1) + " forced=" + r.getBoolean(2) + " policies=" + r.getInt(3));
        }
      }
      c.setAutoCommit(false);
      // As the tenant role with no tenant set: nothing at all.
      asRole(c, null);
      check(count(c, "m14_03.clubs") == 0 && count(c, "m14_03.members") == 0,
          "as m14_tenant with no app.tenant, no club or membership is visible (fail closed)",
          count(c, "m14_03.clubs") + " clubs visible");
      c.rollback();
      asRole(c, "acme");
      int acme = count(c, "m14_03.clubs"), foreign = count(c, "m14_03.clubs WHERE tenant_id <> 'acme'");
      check(acme == 2 && foreign == 0, "as m14_tenant with app.tenant = acme, exactly acme's 2 clubs", acme + " clubs, " + foreign + " of another tenant");
      c.rollback();
      check(!insert(c, "globex", "c-g1", "intruder"), "acme cannot insert a membership labelled globex", "the insert succeeded");
      check(!insert(c, "acme", "c-g1", "intruder"), "acme cannot insert a membership into globex's club c-g1", "the insert succeeded: a foreign key does not see policies");
      c.setAutoCommit(true);
      try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT count(*) FROM m14_03.members WHERE club_id LIKE 'c-g%' AND handle IN ('ada', 'ben')")) {
        r.next();
        check(r.getInt(1) == 0, "after the attacks, no acme user is a member of a globex club", r.getInt(1) + " such memberships");
      }
    } catch (SQLException e) {
      check(false, "the database checks ran", e.getMessage());
    }
    System.exit(failures == 0 ? 0 : 1);
  }

  static void asRole(Connection c, String tenant) throws SQLException {
    try (Statement s = c.createStatement()) { s.execute("SET LOCAL ROLE m14_tenant"); }
    if (tenant != null) try (PreparedStatement s = c.prepareStatement("SELECT set_config('app.tenant', ?, true)")) { s.setString(1, tenant); s.execute(); }
  }

  static int count(Connection c, String from) throws SQLException {
    try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT count(*) FROM " + from)) { r.next(); return r.getInt(1); }
  }

  static boolean insert(Connection c, String tenant, String club, String handle) throws SQLException {
    try {
      asRole(c, "acme");
      try (PreparedStatement s = c.prepareStatement("INSERT INTO m14_03.members (tenant_id, club_id, handle) VALUES (?, ?, ?)")) {
        s.setString(1, tenant); s.setString(2, club); s.setString(3, handle); s.executeUpdate();
      }
      return true;
    } catch (SQLException refused) {
      return false;
    } finally {
      c.rollback();
    }
  }

  static void check(boolean ok, String what, String why) {
    if (ok) System.out.println("ok " + what);
    else { System.out.println("FAIL " + what + ": " + why); failures++; }
  }
}
