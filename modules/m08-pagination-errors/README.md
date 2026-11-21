# Module 8 — Pagination, filtering, sorting and errors

About 7 hours, 7 lectures. By the end you serve million-row lists efficiently and give clients a
predictable, honest error contract.

## Before you start

`bin/course ref up` now also starts **PostgreSQL 18.7** (`postgres:5432` inside the image,
`localhost:5432` from the host; database, user and password `shelfie`). Labs 8.1 to 8.4 use it.

- `bin/course catalog <schema> [rows]` loads the course's deterministic book catalogue
  (`contract/Catalog.java`): 10,000 rows by default, 1,000,000 for lab 8.2, with indexes on
  `(published, id)`, `(title, id)`, `(added_at, id)` and a GIN full-text index. Row *g* is a pure function
  of *g*, so expected outputs are exact. Each lab has its own schema (`m08_01` ...) and the tests reload it.
- `contract/FeedServer.java`: a newest-first feed with offset and cursor paging and a bot that keeps
  posting, for game 8a and the mini-project.
- Labs 8.5, 8.7 and 8.8 are single-file graphql-java programs (`bin/course java`). Labs 8.1, 8.3, 8.4
  and 8.6 are Spring Boot 4.2 apps on :4000 inside the course container, like Module 6.

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 8.1 | [Offset pagination and its limits](lectures/8.1-offset-pagination.md) | [m08/01 Offset paging, and the list that moves](labs/01-offset-drift/README.md) | auto + rubric |
| 8.2 | [Cursor connections](lectures/8.2-cursor-connections.md) | [m08/02 Keyset paging in SQL](labs/02-keyset-sql/README.md) | auto + rubric |
| 8.2 | | [m08/03 Connections in Spring for GraphQL](labs/03-spring-connections/README.md) | auto |
| 8.3 | [Filtering, sorting, search](lectures/8.3-filtering-sorting-search.md) | [m08/04 Filters, sort orders and search](labs/04-filter-sort/README.md) | auto + rubric |
| 8.4 | [The error model](lectures/8.4-error-model.md) | [m08/05 Where does the null go?](labs/05-error-model/README.md) | auto + rubric |
| 8.5 | [Error strategy](lectures/8.5-error-strategy.md) | [m08/06 An error contract](labs/06-error-strategy/README.md) | auto + rubric |
| 8.6 | [Partial results on purpose](lectures/8.6-partial-results.md) | [m08/07 The page that survives](labs/07-partial-results/README.md) | auto |
| 8.7 | [Where error handling is heading](lectures/8.7-error-handling-future.md) | [m08/08 onError: NULL, PROPAGATE, HALT](labs/08-on-error/README.md) | auto + rubric |

## Mini-project and games

| | Solo | Group |
|---|---|---|
| [m08/09 Infinite scroll](labs/09-infinite-scroll/README.md) | 200 books while a bot posts (and deletes) every 20 ms: no duplicates, no gaps, fewest requests | 20-minute build, leaderboard by requests, debrief on deletes |
| [m08/10 Error detective](labs/10-error-detective/README.md) | 6 incidents: cause and origin field, 10 of 12 to pass | Teams of two race, then each explains one incident |
| [m08/11 Terminal book browser](labs/11-project-book-browser/README.md) (mini-project) | A Lanterna TUI paging by cursor, tested on a virtual terminal | Pair: one writes `Browser`, the other the key loop, then swap and review |

## Changes from the plan

- **PostgreSQL joins the course services**, instead of Testcontainers: the course container has no
  Docker socket. One server, one schema per lab, reloaded by the tests.
- **8.2 is two labs**: the SQL first (1,000,000 rows, `EXPLAIN ANALYZE` asserted), then Spring for
  GraphQL's connection support. The probes found that Spring Data JPA 4.2's generated keyset scroll
  filters instead of seeking on a composite sort (`_workbench/2026-11-21-m08-probe/FINDINGS.md`).
- **8.3 uses JPA Specifications** (not jOOQ) and two SQL functions for full-text search. Properties run
  1,000 tries each, and the test adds its own 300-case oracle.
- **8.5's "throw at 5 depths" has six throws** across ten requests, with predictions reported, not graded.
- **8.7's prototype** uses graphql-java's `@experimental_disableErrorPropagation` (added to the parsed
  document) for NULL and `ExecutionInput.cancel()` for HALT. A custom execution strategy was probed and
  propagates partly (lecture 8.7). The "cache-friendliness client test" is an error-aware normalized cache.
- **8.5's status-code experiment** records Spring's behaviour (400/200) against the September 2026
  edition's SHOULDs (422/294); changing it needs a `GraphQlHttpHandler` subclass and is left as a stretch.
- The terminal browser is tested with Lanterna's `DefaultVirtualTerminal`, so the app must use
  `pollInput()` (see the mini-project README).

## Spec references

[Response](https://spec.graphql.org/September2025/#sec-Response),
[Relay Cursor Connections](https://relay.dev/graphql/connections.htm),
[GraphQL over HTTP, September 2026](https://http-spec.graphql.org/September2026/).
RFCs, stages as of 2026-11-21 (`_workbench/2026-11-21-m08-probe/rfcs/`):
[#1163 `onError`](https://rfcs.graphql.org/rfcs/1163/) (RFC 1),
[#1183 response position and sibling errors](https://rfcs.graphql.org/rfcs/1183/) (RFC 3, merged 2026-09-17),
[#1184 no sibling errors after propagation](https://rfcs.graphql.org/rfcs/1184/) (RFC 2),
[#1073 strict error paths](https://rfcs.graphql.org/rfcs/1073/) (RFC 3, merged 2024-04-04),
[#1200 schema coordinate on errors](https://rfcs.graphql.org/rfcs/1200/) (RFC 0).

## Checkpoint

`bin/course checkpoint 08` (see [checkpoint.md](checkpoint.md)): labs 8.1 to 8.8 and the browser
pass, and `error-contract.md` is written. Tag: `checkpoint-08`, with connections, filters and a written
error contract.
