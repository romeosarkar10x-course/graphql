# Lab 9.4 — Lookahead: the whole club page in one SQL statement

**Lecture:** [9.4 Lookahead and smarter SQL](../../lectures/9.4-lookahead-sql.md) · **Grading:** both · **Needs:** postgres

The starter is the batched app from lab 9.3: 8 statements for the dashboard. Now let the root field
look at what the client selected and fetch the whole tree in one statement, with jOOQ's `MULTISET`
(spring-boot-starter-jooq is already in the pom; no code generation needed, use `DSL.name`).

## Do

1. `Query.clubs` takes a `DataFetchingFieldSelectionSet` and builds one jOOQ query from it: joins for
   `owner` and `currentBook`, a correlated count for `memberCount`, and nested `multiset(...)` for
   `ownerPicks`, `members` and `members/reviews` (with `book` joined in). Read each list's `first`
   from the selected field's arguments. Select only what was asked: `clubs { name }` must read only
   the `club` table.
2. Hand the prefetched values to the child fields, so they don't load again. One way: transient fields
   on the entities and a guard at the top of each loader method; another: `localContext`. Fields you
   don't prefetch (`Review.author`) keep working through their loaders.
3. Aliases that select the same list twice with different arguments can't share one `MULTISET`: fall
   back to the loaders for those requests.

## Experiment

Time the dashboard 100 times after a warm-up on the naive, batched and lookahead apps, and record median
and p95 in `notes.md`. Is one statement faster than eight? Read the SQL jOOQ generated (the test turns
on jOOQ's statement log) and explain the result. Then: when does lookahead win, and what does it cost?

## Done when

`bin/course test m09/04` is green and `bin/course grade m09/04` passes the notes.
