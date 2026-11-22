-- Lab 14.5, run as the owner at every start: the lab's data is reset each time.
DROP SCHEMA IF EXISTS m14_05 CASCADE;
CREATE SCHEMA m14_05;
CREATE TABLE m14_05.books (id text PRIMARY KEY, title text NOT NULL);

-- Write model: what commands store.
CREATE TABLE m14_05.reviews (id text PRIMARY KEY, book_id text NOT NULL REFERENCES m14_05.books, author text NOT NULL,
                             rating int NOT NULL, body text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());
-- The outbox: events written in the same transaction as the change, published by the relay afterwards.
CREATE TABLE m14_05.outbox (event_id uuid PRIMARY KEY, aggregate_id text NOT NULL, type text NOT NULL, payload jsonb NOT NULL,
                            created_at timestamptz NOT NULL DEFAULT clock_timestamp(), published_at timestamptz);

-- Read model: what queries read, built only from events.
CREATE TABLE m14_05.review_view (id text PRIMARY KEY, book_id text NOT NULL, author text NOT NULL, rating int NOT NULL,
                                 body text NOT NULL, published_at timestamptz NOT NULL DEFAULT clock_timestamp());
CREATE TABLE m14_05.book_stats (book_id text PRIMARY KEY, review_count int NOT NULL, rating_sum int NOT NULL);
-- Events the projection has applied: Kafka delivers at least once, so each event is applied at most once here.
CREATE TABLE m14_05.processed_events (event_id uuid PRIMARY KEY, processed_at timestamptz NOT NULL DEFAULT now());

INSERT INTO m14_05.books VALUES ('b1', 'The Left Hand of Darkness'), ('b2', 'Piranesi'), ('b3', 'The Remains of the Day');
INSERT INTO m14_05.reviews (id, book_id, author, rating, body) VALUES
  ('r1', 'b1', 'ada', 5, 'The ice crossing.'), ('r2', 'b1', 'ben', 4, 'Estraven deserved better.');
INSERT INTO m14_05.review_view (id, book_id, author, rating, body) VALUES
  ('r1', 'b1', 'ada', 5, 'The ice crossing.'), ('r2', 'b1', 'ben', 4, 'Estraven deserved better.');
INSERT INTO m14_05.book_stats VALUES ('b1', 2, 9), ('b2', 0, 0), ('b3', 0, 0);
