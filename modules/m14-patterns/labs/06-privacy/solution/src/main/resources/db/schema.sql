-- Lab 14.6, run as the owner at every start: the lab's data is reset each time.
DROP SCHEMA IF EXISTS m14_06 CASCADE;
CREATE SCHEMA m14_06;
CREATE TABLE m14_06.users (id text PRIMARY KEY, handle text UNIQUE NOT NULL, name text NOT NULL, email text NOT NULL, phone text);
CREATE TABLE m14_06.books (id text PRIMARY KEY, title text NOT NULL);
CREATE TABLE m14_06.reviews (id text PRIMARY KEY, book_id text NOT NULL REFERENCES m14_06.books, author_id text NOT NULL REFERENCES m14_06.users,
                             rating int NOT NULL, body text NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
                             deleted_at timestamptz);
CREATE SEQUENCE m14_06.review_ids START 100;
CREATE TABLE m14_06.audit_log (id bigserial PRIMARY KEY, at timestamptz NOT NULL DEFAULT clock_timestamp(), actor text NOT NULL,
                               operation text, field text NOT NULL, arguments text[] NOT NULL, outcome text NOT NULL);

INSERT INTO m14_06.users VALUES
  ('u1', 'ada', 'Ada Lovelace', 'ada.lovelace@example.com', '+44 20 7946 0958'),
  ('u2', 'ben', 'Ben Okafor', 'ben.okafor@example.org', NULL),
  ('u5', 'oluwaseun', 'Oluwaseun Adeyemi', 'oluwaseun.adeyemi@example.net', '+234 803 555 0147'),
  ('u9', 'root', 'Shelfie Admin', 'admin@shelfie.dev', NULL);
INSERT INTO m14_06.books VALUES ('b1', 'The Left Hand of Darkness'), ('b2', 'Piranesi');
INSERT INTO m14_06.reviews (id, book_id, author_id, rating, body) VALUES
  ('r1', 'b1', 'u1', 5, 'The ice crossing.'),
  ('r2', 'b1', 'u5', 4, 'Read it on the bus to Ikeja, twice.'),
  ('r3', 'b2', 'u5', 5, 'The tides!'),
  ('r4', 'b2', 'u2', 3, 'Strange, then wonderful.');
