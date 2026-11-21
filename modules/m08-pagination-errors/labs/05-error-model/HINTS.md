# Hints, lab 8.5

1. The source object is the parent's value: in a `User` field's fetcher, `User u = env.getSource()`.
   Throw only for the one id in the table and return the normal value otherwise, or other cases
   change too. `Club.owner` throws always, which is why E6 reuses it.
2. For predicting: walk up from the failing field. If its type has `!`, its parent becomes null too, and
   you ask the same question of the parent, until you reach a position that may be null. A list
   position counts: in `[User!]` an item may not be null, in `[Review]` it may.
3. Request errors (R1 parse, R2 validation, R3 a missing non-null variable) are found before any
   fetcher runs: no `data` key and no `path` on the error, only `locations`. An error's `path` uses
   response keys (aliases) and list indexes, so it is `["a", "owner"]`, not `["club", "owner"]`.
