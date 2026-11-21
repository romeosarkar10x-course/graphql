# Hints, game 7b

1. Client cost is about every client, every day: does a client need hand-written parsing, extra
   requests, or knowledge it can't get from introspection? Lock-in is about the fix: how many clients
   would it break, and can the old and new shape live side by side?
2. Most fixes follow the same migration: add the good shape **next to** the bad one, deprecate the bad
   one with a reason and a date, measure who still uses it, remove it. Names are the hard part, because
   the good name is often taken by the bad field.
3. For card B, look at how lab 7.7's `BookFilter` and Module 8's filter inputs group operators per field
   (`episodes: {min: 10, max: 20}`), instead of one argument per field-and-operator pair.
