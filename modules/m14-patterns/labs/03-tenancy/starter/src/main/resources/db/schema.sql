-- Lab 14.3. Run as the owner at every start (SchemaSetup): the lab's data is reset each time.
DROP SCHEMA IF EXISTS m14_03 CASCADE;
CREATE SCHEMA m14_03;

CREATE TABLE m14_03.tenants (id text PRIMARY KEY, plan text NOT NULL);
CREATE TABLE m14_03.books   (id text PRIMARY KEY, title text NOT NULL);
CREATE TABLE m14_03.clubs   (id text PRIMARY KEY, tenant_id text NOT NULL REFERENCES m14_03.tenants,
                             name text NOT NULL, current_book_id text REFERENCES m14_03.books);
CREATE TABLE m14_03.members (tenant_id text NOT NULL REFERENCES m14_03.tenants, club_id text NOT NULL REFERENCES m14_03.clubs,
                             handle text NOT NULL, joined serial, PRIMARY KEY (club_id, handle));

INSERT INTO m14_03.tenants VALUES ('acme', 'basic'), ('globex', 'premium');
INSERT INTO m14_03.books VALUES ('b1', 'The Left Hand of Darkness'), ('b2', 'Piranesi'), ('b3', 'The Remains of the Day');
INSERT INTO m14_03.clubs VALUES
  ('c-a1', 'acme', 'Acme Readers', 'b1'), ('c-a2', 'acme', 'Acme Night Owls', 'b2'),
  ('c-g1', 'globex', 'Globex Readers', 'b1'), ('c-g2', 'globex', 'Globex Sci-Fi Circle', 'b1');
INSERT INTO m14_03.members (tenant_id, club_id, handle) VALUES
  ('acme', 'c-a1', 'ada'), ('acme', 'c-a1', 'ben'), ('acme', 'c-a2', 'ada'),
  ('globex', 'c-g1', 'max'), ('globex', 'c-g1', 'olu'), ('globex', 'c-g2', 'max');

-- TODO the role every request runs as (m14_tenant), its grants, and row-level security on the tenant tables.
