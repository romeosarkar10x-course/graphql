-- Members are the course token users (bin/course token <handle>); root has the admin role: a librarian.
INSERT INTO toolshed_lending.member (id, handle, display_name, email) VALUES
  ('m-ada', 'ada', 'Ada Okafor', 'ada@example.org'),
  ('m-ben', 'ben', 'Ben Hartley', 'ben@example.org'),
  ('m-max', 'max', 'Max Lindqvist', 'max@example.org'),
  ('m-olu', 'olu', 'Olu Bankole', 'olu@example.org'),
  ('m-root', 'root', 'Rosa Ruiz (librarian)', 'rosa@example.org');

INSERT INTO toolshed_lending.tool_stock (tool_id)
SELECT 't' || lpad(n::text, 2, '0') FROM generate_series(1, 30) n;

-- Times are relative to the start, so OPEN and OVERDUE stay true whenever the seed is loaded.
INSERT INTO toolshed_lending.loan (id, tool_id, member_id, borrowed_at, due_at, returned_at) VALUES
  ('l1', 't01', 'm-ada', now() - interval '60 days', now() - interval '53 days', now() - interval '54 days'),
  ('l2', 't05', 'm-ada', now() - interval '2 days', now() + interval '5 days', NULL),
  ('l3', 't10', 'm-ben', now() - interval '10 days', now() - interval '3 days', NULL),
  ('l4', 't14', 'm-max', now() - interval '1 day', now() + interval '6 days', NULL),
  ('l5', 't21', 'm-olu', now() - interval '20 days', now() - interval '13 days', NULL),
  ('l6', 't02', 'm-ben', now() - interval '30 days', now() - interval '23 days', now() - interval '25 days');
