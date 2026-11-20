# Hints, Predict the response

1. A response has `data`, `errors` or both. A document that fails validation never executes, so it has no `data` key at all. A field that finds nothing and is nullable is simply `null`.
2. Check every field's arguments in the docs pane: `members(first: Int = 10)` and `reviews(last: Int = 5)` apply even when you pass nothing. A variable with a default needs no value from the caller.
3. Two fields with the same response key and arguments merge into one object. A field is included only if `@skip` is false *and* `@include` is true. An inline fragment that does not match the result's type contributes nothing, but the object is still there.
