# Lab 9.3 — Batch loading with java-dataloader and Spring

**Lecture:** [9.3 java-dataloader and Spring](../../lectures/9.3-java-dataloader-spring.md) · **Grading:** both · **Needs:** postgres

The starter is your lab 9.1 app: 1,250 statements for the dashboard. Get it to one statement per field
per level, keeping every response the same.

## Do

1. Fields without arguments (`Club.owner`, `Club.currentBook`, `Club.memberCount`, `Review.book`,
   `Review.author`): `@BatchMapping`, one method that takes all the parents and returns a map. A club
   without a current book is simply missing from the map.
2. Fields with arguments (`Club.members(first)`, `Reader.reviews(first)`): the argument has to be part
   of the key, so register a loader in `BatchLoaderRegistry` keyed by a record such as
   `(clubId, first)`, and load from a `@SchemaMapping` method taking a `DataLoader` parameter (named
   after the loader). One SQL statement serves all keys: a window function
   (`row_number() over (partition by ...)`) with the largest `first`, and each key keeps its own prefix.
3. `Club.ownerPicks(first)` is a chain: the owner's reviews (the `reviews` loader), then their books (a
   `books` loader by id), in `thenCompose`. Make it answer. If it hangs, read lecture 9.3 again.

The test checks the counts (at most 9 for the dashboard, at most 4 for three levels of lists), that
two `members` aliases with different `first` both get the right members, that `ownerPicks` answers
within 10 seconds, and that the dashboard data is byte-for-byte the naive version's
(`contract/dashboard.expected.json`).

## Experiment

The same dashboard three ways: naive, with loaders, and without the chaining fix. Record statements and
warm latency for each in `notes.md`, with a bar chart (text is fine), and explain which fields needed
which mechanism.

## Done when

`bin/course test m09/03` is green and `bin/course grade m09/03` passes the notes.
