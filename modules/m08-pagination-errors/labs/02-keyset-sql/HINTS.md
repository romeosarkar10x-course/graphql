# Hints, lab 8.2

1. Look at the `Index Cond` and `Filter` lines in the OR form's plan. PostgreSQL can only start an index
   scan at a position when the condition is one range on the index columns. `a > x OR (a = x AND b > y)`
   is two ranges joined by OR, so the planner walks the index from the start and filters.
2. SQL has row values: `(published, id) > (?, ?)` compares lexicographically, exactly the order of the
   `(published, id)` index, and PostgreSQL turns it into one `Index Cond`. Backwards is `<` with
   `order by published desc, id desc`; reverse the list in Java afterwards.
3. For the cursor, base64url-encode `"v1:" + published + ":" + id` (`Base64.getUrlEncoder().withoutPadding()`).
   In `decode`, catch the decoder's `IllegalArgumentException`, then check for exactly 3 parts and the
   `v1` prefix, and turn any `NumberFormatException` into an `IllegalArgumentException` too. A
   null `after` can mean the position `(Integer.MIN_VALUE, Long.MIN_VALUE)`, so one query covers both cases.
