-- Lab 14.7, run as the owner at every start: the lab's data is reset each time.
DROP SCHEMA IF EXISTS m14_07 CASCADE;
CREATE SCHEMA m14_07;
CREATE TABLE m14_07.books (id text PRIMARY KEY, title text NOT NULL, author text, published_year int);
INSERT INTO m14_07.books VALUES
  ('b1', 'The Left Hand of Darkness', 'Ursula K. Le Guin', 1969), ('b2', 'Piranesi', 'Susanna Clarke', 2020),
  ('b3', 'The Remains of the Day', 'Kazuo Ishiguro', 1989);

-- The metadata: which columns of m14_07.books are fields of the GraphQL type Book, and how they are typed.
-- graphql_type is one of STRING, INT, FLOAT, BOOLEAN. id and title are always there and are not listed.
CREATE TABLE m14_07.fields (type_name text NOT NULL, field_name text NOT NULL, graphql_type text NOT NULL,
                            column_name text NOT NULL, description text, position serial,
                            PRIMARY KEY (type_name, field_name));
INSERT INTO m14_07.fields (type_name, field_name, graphql_type, column_name, description) VALUES
  ('Book', 'author', 'STRING', 'author', 'Main author, as credited.'),
  ('Book', 'publishedYear', 'INT', 'published_year', 'Year of first publication.');
