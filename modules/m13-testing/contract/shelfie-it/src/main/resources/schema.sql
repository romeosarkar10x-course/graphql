CREATE SCHEMA IF NOT EXISTS m13_it;
SET search_path TO m13_it;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS book;
CREATE TABLE book (id text PRIMARY KEY, title text NOT NULL);
CREATE TABLE review (
  id         bigserial PRIMARY KEY,
  book_id    text NOT NULL REFERENCES book (id),
  author     text NOT NULL,
  rating     int  NOT NULL CHECK (rating BETWEEN 1 AND 5),
  body       text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (book_id, author)
);
