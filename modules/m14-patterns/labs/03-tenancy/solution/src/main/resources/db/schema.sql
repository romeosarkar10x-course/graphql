-- Lab 14.3. Run as the owner at every start (SchemaSetup): the lab's data is reset each time.
DROP SCHEMA IF EXISTS m14_03 CASCADE;
CREATE SCHEMA m14_03;

CREATE TABLE m14_03.tenants (id text PRIMARY KEY, plan text NOT NULL);
CREATE TABLE m14_03.books   (id text PRIMARY KEY, title text NOT NULL);
CREATE TABLE m14_03.clubs   (id text PRIMARY KEY, tenant_id text NOT NULL REFERENCES m14_03.tenants,
                             name text NOT NULL, current_book_id text REFERENCES m14_03.books, UNIQUE (tenant_id, id));
-- The membership's tenant must be its club's tenant: a foreign key on both columns. Foreign key checks do not see
-- row-level security, so without it a tenant could insert a membership pointing at another tenant's club.
CREATE TABLE m14_03.members (tenant_id text NOT NULL, club_id text NOT NULL, handle text NOT NULL, joined serial,
                             PRIMARY KEY (club_id, handle), FOREIGN KEY (tenant_id, club_id) REFERENCES m14_03.clubs (tenant_id, id));

INSERT INTO m14_03.tenants VALUES ('acme', 'basic'), ('globex', 'premium');
INSERT INTO m14_03.books VALUES ('b1', 'The Left Hand of Darkness'), ('b2', 'Piranesi'), ('b3', 'The Remains of the Day');
INSERT INTO m14_03.clubs VALUES
  ('c-a1', 'acme', 'Acme Readers', 'b1'), ('c-a2', 'acme', 'Acme Night Owls', 'b2'),
  ('c-g1', 'globex', 'Globex Readers', 'b1'), ('c-g2', 'globex', 'Globex Sci-Fi Circle', 'b1');
INSERT INTO m14_03.members (tenant_id, club_id, handle) VALUES
  ('acme', 'c-a1', 'ada'), ('acme', 'c-a1', 'ben'), ('acme', 'c-a2', 'ada'),
  ('globex', 'c-g1', 'max'), ('globex', 'c-g1', 'olu'), ('globex', 'c-g2', 'max');

-- The role every request runs as. NOLOGIN: only the app can become it (SET LOCAL ROLE inside a transaction).
DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'm14_tenant') THEN CREATE ROLE m14_tenant NOLOGIN; END IF;
END $$;
GRANT USAGE ON SCHEMA m14_03 TO m14_tenant;
GRANT SELECT ON m14_03.tenants, m14_03.books, m14_03.clubs TO m14_tenant;
GRANT SELECT, INSERT ON m14_03.members TO m14_tenant;
GRANT USAGE ON SEQUENCE m14_03.members_joined_seq TO m14_tenant;

-- Row-level security: a row is visible, and may be written, only when its tenant is the transaction's app.tenant.
-- No app.tenant set means no rows: current_setting(..., true) is NULL and NULL = x is never true. FORCE makes the
-- table's owner obey the policies too.
ALTER TABLE m14_03.tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE m14_03.tenants FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON m14_03.tenants USING (id = current_setting('app.tenant', true));
ALTER TABLE m14_03.clubs ENABLE ROW LEVEL SECURITY;
ALTER TABLE m14_03.clubs FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON m14_03.clubs
  USING (tenant_id = current_setting('app.tenant', true)) WITH CHECK (tenant_id = current_setting('app.tenant', true));
ALTER TABLE m14_03.members ENABLE ROW LEVEL SECURITY;
ALTER TABLE m14_03.members FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON m14_03.members
  USING (tenant_id = current_setting('app.tenant', true)) WITH CHECK (tenant_id = current_setting('app.tenant', true));
