// Does a foreign key check see row-level security? Single-column FK, tenant role, app.tenant = acme,
// inserting a membership (tenant acme) that points at globex's club.
import java.sql.*;

public class FkProbe {
  public static void main(String[] a) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:postgresql://postgres:5432/shelfie", "shelfie", "shelfie"); Statement s = c.createStatement()) {
      s.execute("""
        DROP SCHEMA IF EXISTS p14 CASCADE; CREATE SCHEMA p14;
        CREATE TABLE p14.clubs (id text PRIMARY KEY, tenant_id text NOT NULL);
        CREATE TABLE p14.members (tenant_id text NOT NULL, club_id text NOT NULL REFERENCES p14.clubs, handle text NOT NULL);
        INSERT INTO p14.clubs VALUES ('c-a1', 'acme'), ('c-g1', 'globex');
        DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'p14_tenant') THEN CREATE ROLE p14_tenant NOLOGIN; END IF; END $$;
        GRANT USAGE ON SCHEMA p14 TO p14_tenant; GRANT SELECT ON p14.clubs TO p14_tenant; GRANT SELECT, INSERT ON p14.members TO p14_tenant;
        ALTER TABLE p14.clubs ENABLE ROW LEVEL SECURITY; ALTER TABLE p14.clubs FORCE ROW LEVEL SECURITY;
        CREATE POLICY t ON p14.clubs USING (tenant_id = current_setting('app.tenant', true));
        ALTER TABLE p14.members ENABLE ROW LEVEL SECURITY; ALTER TABLE p14.members FORCE ROW LEVEL SECURITY;
        CREATE POLICY t ON p14.members USING (tenant_id = current_setting('app.tenant', true)) WITH CHECK (tenant_id = current_setting('app.tenant', true));
        """);
      c.setAutoCommit(false);
      s.execute("SET LOCAL ROLE p14_tenant");
      s.execute("SELECT set_config('app.tenant', 'acme', true)");
      ResultSet r = s.executeQuery("SELECT count(*) FROM p14.clubs WHERE id = 'c-g1'"); r.next();
      System.out.println("as acme, visible rows for c-g1: " + r.getInt(1));
      try {
        s.executeUpdate("INSERT INTO p14.members VALUES ('acme', 'c-g1', 'intruder')");
        System.out.println("single-column FK: INSERT of acme membership into globex club c-g1 SUCCEEDED (FK check ignores RLS)");
      } catch (SQLException e) { System.out.println("single-column FK: insert refused: " + e.getMessage()); }
      c.rollback();
      // superuser, no FORCE matters: a superuser bypasses RLS entirely
      c.setAutoCommit(true);
      r = s.executeQuery("SELECT count(*) FROM p14.clubs"); r.next();
      System.out.println("as shelfie (superuser), no app.tenant, rows visible: " + r.getInt(1));
      r = s.executeQuery("SELECT rolsuper FROM pg_roles WHERE rolname = current_user"); r.next();
      System.out.println("shelfie is superuser: " + r.getBoolean(1));
      s.execute("DROP SCHEMA p14 CASCADE");
    }
  }
}
