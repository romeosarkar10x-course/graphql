SET search_path TO m13_it;
INSERT INTO book (id, title) VALUES
  ('b1', 'The Left Hand of Darkness'), ('b2', 'Piranesi'), ('b3', 'The Remains of the Day'),
  ('b4', 'The Name of the Rose'), ('b5', 'Good Omens');
INSERT INTO review (book_id, author, rating, body, created_at) VALUES
  ('b1', 'u1', 5, 'Changed how I think about gender in fiction.', '2026-10-02T21:10:00Z'),
  ('b1', 'u2', 4, 'Slow first half, devastating second.', '2026-10-05T08:30:00Z'),
  ('b1', 'u3', 5, 'The ice crossing chapter.', '2026-10-09T22:45:00Z'),
  ('b2', 'u1', 5, 'A house with infinite halls.', '2026-06-01T10:00:00Z'),
  ('b2', 'u6', 4, 'Listened twice in a row.', '2026-06-03T12:00:00Z'),
  ('b2', 'u7', 4, 'Statues!', '2026-06-04T12:00:00Z');
