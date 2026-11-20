# Mini-project 4 — Undo everything

**Lectures:** [4.2](../../lectures/4.2-inputs-payloads.md)–[4.4](../../lectures/4.4-idempotency.md) · **Grading:** auto + rubric · **Needs:** nothing · **Kind:** mini-project

Gmail's "Undo send", Slack's "undo" on a deleted message: the server returns a token, the client shows
a button for a few seconds, and one more mutation reverses the change. Every Shelfie mutation in
`undo.graphqls` returns an `undoToken`; `undo(input: {token})` reverses it.

## The rules

1. Every mutation that **changes** something returns a fresh `undoToken`. A no-op (joining a club you
   are in, renaming to the same name) returns `undoToken: null`. Errors return `null`.
2. `undo(token)` reverses exactly that change, **once**: a second use is `USED`, an unknown token
   `NOT_FOUND`.
3. Undo never overwrites someone else's later change: if the state is no longer what the change left
   behind (the club was renamed again, the review was deleted, the user rejoined), it is a `CONFLICT`
   and nothing changes. Once the state is back (the later change was itself undone), it works again.
4. `undo`'s payload has its own `undoToken`: undoing the undo is a redo.
5. A deleted review comes back with the **same id**.

## Design: the command pattern

Model each change as a record implementing a `sealed interface Command` with `apply(State)`,
`inverse()` and `conflict(State)` (what must be true for the inverse to be safe). A mutation builds a
command, applies it, stores it under a new token. `undo` looks the command up, checks `conflict`,
applies `inverse()`, and stores *that* under a new token.

## Do

1. `Undo.java`: the commands, the token store, `undo`. Keep `Undo.graphQL()`; the test scripts in
   `tests/scripts/` run against it (the viewer comes from the GraphQL context).
2. `DESIGN.md`.

## Done when

`bin/course test m04/08` is green and `bin/course grade m04/08` gives 7/10 or more.

## Group variant

Pairs: one person writes the commands, the other writes three new test scripts in the
`tests/scripts/` format that try to make undo overwrite someone's change. Swap and fix.
