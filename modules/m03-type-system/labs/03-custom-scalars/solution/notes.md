# Lab 3.3 notes (reference)

## The starter's table

See `starter-table.tsv` (generated from the starter with the extended scalars registered). Every row
except 12 and 13 shows a defect; literal and variable disagree on rows 01, 02, 03, 04, 05, 07, 08, 09,
10 and 14. Causes:

- `parseLiteral` returns the raw string: `978-0-441-47812-6`, `ISBN 9780441478125` and the
  Arabic-Indic digits come back unnormalised and unchecked (rows 01, 07, 10).
- `parseValue` does `String.valueOf(in)`: a JSON number (02) and even an object, whose `toString` is
  `{isbn=9780441478125}` (14), become valid-looking strings.
- `normalise` strips instead of rejecting and has no checksum: rows 01, 06, 09, 15 are accepted by
  both paths.

## Your table

A literal is part of the document, so it is checked during **validation**, before anything executes;
graphql-java reports `Validation error (WrongType@[check])`. A variable is checked when variables are
**coerced**, also before execution: `Variable 'i' has an invalid value`. Either way the client gets a
request error with no `data`, and no resolver ran.

## Question 1

Output is a promise to clients: an `ISBN!` they can trust without re-checking. Passing through a bad
value moves the bug from your server to every client. Throwing makes it a field error you see in logs.

## Question 2

Scalar: one definition, every argument and field gets the rule, introspection documents it; but every
client needs to know the format, and codegen maps it to `String` unless configured. String plus rule:
clients need nothing; but each argument must remember to apply the rule, and output carries no promise.
