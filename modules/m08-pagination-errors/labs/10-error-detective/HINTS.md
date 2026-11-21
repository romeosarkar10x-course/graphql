# Hints, game 8b

1. Read the response before the ticket. No `data` key means a request error (validation, variables):
   nothing ran, so the origin is the part of the schema the request got wrong. A `path` is the
   origin of a field error, *not* the field the user noticed. Propagation may have nulled
   something much higher up.
2. In incident 2, two errors are logged. Only one `errorId` matches the response. In incident 6, a
   `NullValueInNonNullableField` on *every* field of an object is rarely a data problem: what would
   make `name` and `handle` both null at once?
3. Incident 5 has no path at all. Read the timestamps: when was the budget hit, and when did the
   engine notice? The field to name is the one whose fetcher was running. For incident 4,
   the coordinate of an argument is `Type.field(argument:)`.
