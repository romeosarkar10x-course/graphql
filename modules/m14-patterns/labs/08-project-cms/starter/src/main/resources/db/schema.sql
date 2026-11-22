-- Mini-project 14.8. Content types and entries survive restarts: nothing is dropped here.
CREATE SCHEMA IF NOT EXISTS m14_08;
CREATE TABLE IF NOT EXISTS m14_08.content_types (name text PRIMARY KEY, plural text UNIQUE NOT NULL, description text,
                                                 position serial);
CREATE TABLE IF NOT EXISTS m14_08.content_fields (type_name text NOT NULL REFERENCES m14_08.content_types ON DELETE CASCADE,
                                                  name text NOT NULL, kind text NOT NULL, ref text, required boolean NOT NULL,
                                                  description text, position serial, PRIMARY KEY (type_name, name));
CREATE TABLE IF NOT EXISTS m14_08.entries (id text PRIMARY KEY, type_name text NOT NULL, data jsonb NOT NULL,
                                           created_at timestamptz NOT NULL DEFAULT clock_timestamp());
CREATE SEQUENCE IF NOT EXISTS m14_08.entry_ids;
