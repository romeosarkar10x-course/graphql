-- Full-text search as two SQL functions, so a JPA Specification can call them with cb.function(...).
-- Single-statement SQL functions are inlined by the planner, so the GIN index on
-- to_tsvector('english', title || ' ' || blurb) (created by the catalogue) still applies.
create or replace function book_matches(title text, blurb text, q text) returns boolean
  language sql immutable as $$ select to_tsvector('english', title || ' ' || blurb) @@ websearch_to_tsquery('english', q) $$;
create or replace function book_rank(title text, blurb text, q text) returns real
  language sql immutable as $$ select ts_rank(to_tsvector('english', title || ' ' || blurb), websearch_to_tsquery('english', q)) $$;
