# Lab 8.2 — Keyset paging in SQL

**Lecture:** [8.2 Cursor connections](../../lectures/8.2-cursor-connections.md) · **Grading:** both · **Needs:** postgres (`bin/course ref up`)

A cursor connection is only as good as the SQL under it. This lab skips GraphQL. You write the keyset
(seek) queries a connection needs, make the cursors opaque, and prove with `EXPLAIN ANALYZE` that page
45,000 costs the same as page 1. Lab 8.3 puts the result behind Spring for GraphQL.

The table is `m08_02.book`, 1,000,000 rows from the course catalogue (`bin/course catalog`), ordered by
`(published, id)`. Only 125 distinct years exist, so `published` alone is not a key. `id` breaks ties,
which makes every position unique.

## Do

`Keyset.java` (single file; the harness calls its methods, so keep the signatures):

1. **Cursors.** `encode(published, id)` returns an opaque string: unpadded base64url with a version
   prefix inside (`v1:1999:424242`). `decode` gives the `Position` back and throws
   `IllegalArgumentException` for anything `encode` did not produce, including the old plain `1999:424242`.
2. **Forward.** `FORWARD_SQL` takes three parameters, published, id and limit, and returns the rows strictly after
   that position. `forward(c, first, after)` fetches `first + 1` rows: the extra row answers
   `hasNextPage` and is not returned. `hasPreviousPage` is `true` whenever `after` was given. The spec
   allows that shortcut, and Spring for GraphQL takes a cheaper one (see lecture 8.2).
3. **Backward.** `BACKWARD_SQL` and `backward(c, last, before)` return the `last` rows before the cursor,
   **in ascending order**. A null `before` means the end of the list.
4. **The plan.** At row 900,000 neither query may Seq Scan, Sort, or remove more than 100 rows by filter.
   The starter's `FORWARD_SQL` is the shape Spring Data JPA 4.2 generates (`published > ? or published = ? and id > ?`):
   correct rows, wrong plan. Find out why, then fix it.

`offsetPage` is the baseline, given. Don't grade yourself on it.

## Experiment

`notes.md`: fill the table with `EXPLAIN ANALYZE` at offsets 0, 10,000, 100,000 and 900,000 for
`OFFSET`, your keyset query and the starter's OR form (`Execution Time` and the rows the scan read).
Get a shell in the course image with `bin/course shell`, then run
`java -cp "$(source tools/testlib.sh; lib_cp)" Explain.java`, or paste the SQL into
`docker exec -it gqlcourse-postgres-1 psql -U shelfie`, where `set search_path = m08_02;` comes first.
Then answer the three questions in the file.

## Done when

`bin/course test m08/02` is green (23 checks; it prints a timing line from Java too), and
`bin/course grade m08/02` passes the notes.
