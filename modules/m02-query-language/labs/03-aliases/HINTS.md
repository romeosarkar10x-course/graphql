# Hints, lab 2.3

1. An alias goes before the field name with a colon: `left: club(id: $a) { ... }`. The response key becomes `left`. You can alias any field, at any depth, including scalars.
2. You need two aliased root fields with the same selection, and one more alias inside `currentBook` so that `averageRating` arrives as `rating`.
3. For the collision, drop the aliases: two plain `club(...)` fields with `$a` and `$b` share the response key `club`. Validation errors come back with no `data` at all.
