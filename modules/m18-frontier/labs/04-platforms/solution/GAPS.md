# What PostGraphile does not give Shelfie

Reference notes. Observations are from PostGraphile 5.2.1 (amber preset, graphql 16.15.0) on Module 9's club
dataset, 2026-11-23 (`_workbench/2026-11-23-m18-probe/postgraphile/`). Viaduct's starter was not run (see the
README).

## Schema shape

- **Names come from SQL.** `clubByRowId`, `readerByOwnerId`, `bookByCurrentBookId`, `membershipsByClubId`: a
  client learns the foreign key columns. Shelfie's hand-built schema says `club(id:)`, `owner`, `currentBook`,
  `members`. Renaming `owner_id` in a migration renames `readerByOwnerId` and breaks every client, with no
  deprecation step: the database schema has become the API contract (Module 7's evolution rules do not apply).
- **Vocabulary.** The table is `reader`, so the type is `Reader`; Shelfie's clients say `User`. Smart tags can
  rename, but each rename is now config to maintain.
- **Two ids.** Every type has an opaque global `id` (for `node(id:)`) and `rowId: BigInt!`, the primary key as a
  string-encoded big integer. Clients get two ways to identify a club.
- **Nullability from SQL, but not quite.** NOT NULL columns become non-null (`name: String!`, `ownerId:
  BigInt!`), but the relation is nullable: `readerByOwnerId: Reader` although `owner_id` is `not null references
  reader`. Module 7's nullability strategy (non-null where the product guarantees it) has to be re-imposed.

## Filtering and ordering

`orderBy` and `condition` exist only for indexed columns: `ReviewOrderBy` has `BOOK_ID`, `AUTHOR_ID`,
`CREATED_AT`, `ROW_ID` and `NATURAL`, but no `RATING_DESC`, and there is no filter on `rating` or `body`. That is a
deliberate default (no accidental sequential scans), and it means a product screen ("top-rated reviews") needs
either a new index or a hand-written function. There is no text search. Pages have no cap: `allReviews(first:
1000)` returns 1,000 rows.

## Security

- **Authorization.** Everything the database user can see is public. PostGraphile's documented answer is PostgreSQL
  row-level security, with each request's claims passed to the database as settings (`pgSettings`; from its
  documentation, not tried here); Shelfie's rules (Module 10: a
  private club's members only for members) would be policies in SQL, tested in SQL.
- **Demand control.** The server options have `maxRequestLength` but no depth or cost limit; a query of nested
  connections with `first: 1000` at each level is accepted. Module 10's limits need a plugin or a gateway.
- **Trusted documents.** None by default; any client can send any query (Module 10.5).
- **Errors.** Unexpected errors are masked ("An error occurred (logged with hash: ...)"), which is right for
  production, but it also masks a plain client mistake: a mutation sent to the read-only API is not refused by
  validation (graphql-js 16.15 lacks the September 2025 rule "Operation Type Existence", 5.2.1.1) and comes back
  as a masked planning error, HTTP 400.

## Writes

The amber preset generated 27 mutations: create, update and delete for every table, by primary key and by
unique key (`deleteReaderByHandle`). They are row operations, not product operations: no `userErrors` (Module 4),
no idempotency for `joinClub`, no rule that the owner cannot leave, no check that a review is 10 to 2,000
characters except what a CHECK constraint says. Removing them all (`defaultBehavior: "-insert -update -delete"`)
and writing a few real mutations as PostgreSQL functions, or in a Java service, is the usual path.

## Another platform

**Hasura** (graphql-engine v2, Apache-2.0, v2.51.9 of 2026-11-19): also generates from PostgreSQL, but
authorization is declarative per role in its metadata (row and column permissions), and it adds remote schemas
and actions to call hand-written services (from its documentation, not run here). **Viaduct** (Airbnb, Apache-2.0, v3.0.0) is the opposite design: not
generated from a database but a central schema with resolvers hosted inside it (Kotlin or Java,
`@resolver` fields), where resolvers call each other through GraphQL fragments ("re-entrancy"). It is what a
company builds when it wants one graph and hand-written logic, without a federation router.

## Verdict

A generated API is the right call for an internal admin tool, a back office, a prototype or a data team's
exploration: the database is already the contract, the users are trusted, and speed matters more than shape.
It is the wrong call for Shelfie's public clients: they would be coupled to the table layout, authorization would
move into SQL policies the API team does not own, and every product rule would have to be re-added. A middle path:
generate for internal reads, hand-build the client-facing schema (or put the generated one behind federation as
one subgraph, renamed and restricted).
