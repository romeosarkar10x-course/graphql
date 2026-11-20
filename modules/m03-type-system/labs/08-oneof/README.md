# Lab 3.8 — `BookLookup @oneOf`

**Lecture:** [3.8 Input types and `@oneOf`](../../lectures/3.8-inputs-oneof.md) · **Grading:** auto + rubric · **Needs:** nothing

Before `@oneOf`, "find a book by id **or** ISBN **or** title" meant three nullable fields and a resolver
that hoped the client sent one. The starter is that resolver: send two keys and it silently ignores one.

## Do

1. Make `BookLookup` a `@oneOf` input in `schema.graphqls`, and rewrite `find` to switch on the single
   key it receives. graphql-java now rejects bad shapes before your code runs.
2. `tests/cases.jsonl` (in the lab folder under `modules/`) has 14 lookups: literals, variables, nulls,
   two keys, no keys. Run each and fill the table in `notes.md`: outcome and stage (validation,
   variable coercion, execution). The test compares your outcomes with the reference.
3. `cycle.graphqls`: write `input A @oneOf { a: A }` (plus a `Query` that uses it) and confirm
   `Lookup.java --schema cycle.graphqls` is rejected. Read the message.
4. `inhabited.graphqls`: keep `A` self-referencing and `@oneOf`, but make it buildable.
5. `notes.md`.

## Done when

`bin/course test m03/08` is green and `bin/course grade m03/08` gives 7/10 or more.
