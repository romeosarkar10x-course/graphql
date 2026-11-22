# Lightning talk: an API you did not write

Reference notes: an outline of a passing talk, not a script to read out. Read on 2026-11-23. Version:
PostGraphile 5.2.1.

## The one idea

A generated GraphQL API is not free; it's the database schema published as your API contract.

## Script outline (about 650 words when written out)

1. (30 s) Hook: "Seventeen modules of hand-built Shelfie. What if we had pointed a tool at the database instead?"
2. (60 s) The demo: PostGraphile on Module 9's club data. One command, 20 query fields, 27 mutations, including
   `deleteReaderByHandle`, and a SQL helper function (`pick`) published as a Query field.
3. (90 s) What you get for free: connections with `totalCount` and cursors, `node(id:)`, filters and ordering
   on indexed columns, foreign keys as fields (`readerByOwnerId`).
4. (90 s) What you do not get: product names and nullability (the owner relation is nullable although the column
   is NOT NULL), errors as data, authorization outside SQL row-level security, any depth or cost limit, and real
   mutations instead of row operations.
5. (30 s) Two lines of config make it read-only and hide the helper; the rest is design work.
6. (20 s) Verdict and date: great for admin tools and prototypes; for public clients, generate internally and
   hand-build the contract. PostGraphile 5.2.1, 2026-11-23.

## Slides

1. The command and the count: 20 queries, 27 mutations
2. `readerByOwnerId: Reader` beside `owner_id bigint not null references reader`
3. What is free / what is not, two columns
4. The two config lines
5. When to use it

## Demo

`{ clubByRowId(rowId: "1") { name readerByOwnerId { handle } membershipsByClubId(first: 3) { totalCount } } }`,
then the same API's `deleteReaderByHandle` in the schema, then the read-only config and the mutation type gone.

## Sources

PostGraphile 5.2.1 (npm, MIT) run on 2026-11-23 (`_workbench/2026-11-23-m18-probe/postgraphile/`); lab 18.4's
`GAPS.md`.

## After the talk

Likely questions: "Why not Hasura?" (declarative permissions per role in its metadata: an authorization model,
not just generation); "Can it be a federation subgraph?" (as one subgraph behind renames and restrictions,
yes in principle; not tried in this course).
